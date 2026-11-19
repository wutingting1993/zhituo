package com.cmbchina.application.monitor.service;

import com.cmbchina.application.monitor.api.MonitorApplicationService;
import com.cmbchina.application.monitor.api.dto.MonitorQuery;
import com.cmbchina.application.monitor.api.dto.MonitorRefresh;
import com.cmbchina.domain.monitor.support.MonitorDataSupport;
import com.cmbchina.infrastructure.common.ResponseEnum;
import com.cmbchina.infrastructure.common.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** 校验监控查询，聚合同一统计桶并补全业务名称。 */
@Service
public class InterfaceMonitorService implements MonitorApplicationService {
    private static final int LOOKBACK_MONTHS = 6;
    private static final int COVERAGE_BUCKET_SECONDS = 600;
    private static final long MAX_OFFSET = 4294967295L;
    private static final ZoneId BEIJING = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSXXX");
    private static final Set<String> GROUPS = ImmutableSet.of("interface", "expert", "total");
    private static final Set<String> PERIODS = ImmutableSet.of("day", "week", "month");
    private static final String[] RATES = {"error_rate", "timeout_rate", "cache_hit_rate", "p95_millis", "p99_millis"};
    private static final String[] COUNTS = {"started_count", "success_count", "failure_count", "timeout_count", "cancelled_count", "cache_read_count",
            "cache_hit_count", "expert_count", "interface_count"};

    @Autowired
    private MonitorDataSupport data;

    @Autowired
    private ObjectMapper mapper;

    /** 返回已覆盖区间内的统计与数据完成时间。 */
    @Override
    public JsonNode query(MonitorQuery request) {
        validate(request);
        ObjectNode range = mapper.createObjectNode().put("start_time", midnight(request.getStartDate()))
                .put("end_time", midnight(request.getEndDate().plusDays(1))).put("bucket_seconds", request.getBucketSeconds());
        ObjectNode filters = range.deepCopy().put("group_by", request.getGroupBy()).put("interface_id", orEmpty(request.getInterfaceId()))
                .put("agent_id", orEmpty(request.getAgentId())).put("unattributed_only", request.isUnattributedOnly() ? 1 : 0);
        ArrayNode rows = data.query("interface-monitor-statistics", filters);
        ArrayNode coverage = data.query("interface-monitor-offline-coverage", range);
        return assemble(request, rows, coverage);
    }

    /** 手动重算指定周期，无页面回看窗口限制。 */
    @Override
    public JsonNode refresh(MonitorRefresh request) {
        if (!PERIODS.contains(request.getPeriodType()) || request.getOffset() < 0 || request.getOffset() > MAX_OFFSET) {
            throw new BusinessException(ResponseEnum.VALIDATION_ERROR.getCode(), "离线更新周期或偏移量无效", null);
        }
        return data.refresh(mapper.createObjectNode().put("period_type", request.getPeriodType()).put("offset", request.getOffset()));
    }

    /** 保持查询和重算的不同时间边界。 */
    private void validate(MonitorQuery request) {
        validateDates(request.getStartDate(), request.getEndDate());
        if (!GROUPS.contains(request.getGroupBy()) || request.getBucketSeconds() < 0 || request.getBucketSeconds() % COVERAGE_BUCKET_SECONDS != 0) {
            throw new BusinessException(ResponseEnum.VALIDATION_ERROR.getCode(), "监控分组或趋势粒度无效", null);
        }
        if (request.isUnattributedOnly() && !orEmpty(request.getAgentId()).isEmpty()) {
            throw new BusinessException(ResponseEnum.VALIDATION_ERROR.getCode(), "未归属筛选不能同时选择领域专家", null);
        }
    }

    /** 校验北京时间下页面可查询的日期范围。 */
    private void validateDates(LocalDate start, LocalDate end) {
        LocalDate today = LocalDate.now(BEIJING);
        if (start == null || end == null || end.isBefore(start) || end.isAfter(today) || start.isBefore(today.minusMonths(LOOKBACK_MONTHS))) {
            throw new BusinessException(ResponseEnum.VALIDATION_ERROR.getCode(), "请选择最近六个月内的有效日期范围", null);
        }
    }

    /** 将两种口径和覆盖信息组装为页面使用的数据。 */
    private ObjectNode assemble(MonitorQuery request, ArrayNode rows, ArrayNode coverage) {
        Map<String, ObjectNode> items = Maps.newLinkedHashMap();
        for (JsonNode row : rows) {
            addMetric(items, row);
        }
        if (request.getBucketSeconds() > 0 && "total".equals(request.getGroupBy())) {
            for (JsonNode bucket : coverage) {
                addCoveredBucket(items, bucket);
            }
        }
        Map<String, String> names = loadNames(request.getGroupBy(), items);
        ArrayNode result = mapper.createArrayNode();
        for (ObjectNode item : items.values()) {
            String key = item.path("key").asText();
            item.put("name", displayName(request.getGroupBy(), key, names));
            fillScope(item, "execution");
            fillScope(item, "upstream");
            result.add(item);
        }
        ObjectNode response = mapper.createObjectNode();
        response.set("items", result);
        response.set("coverage", normalizeCoverage(coverage));
        String completedAt = "";
        for (JsonNode bucket : coverage) {
            String value = bucket.path("statistics_completed_at").asText("");
            if (value.compareTo(completedAt) > 0) {
                completedAt = value;
            }
        }
        response.put("statisticsCompletedAt", completedAt.isEmpty() ? null : completedAt);
        return response;
    }

    /** 将覆盖秒数统一为 JSON 数字。 */
    private ArrayNode normalizeCoverage(ArrayNode coverage) {
        ArrayNode normalized = mapper.createArrayNode();
        for (JsonNode bucket : coverage) {
            ObjectNode row = mapper.createObjectNode();
            row.set("available_from", bucket.get("available_from"));
            row.set("available_through", bucket.get("available_through"));
            row.put("effective_seconds", bucket.path("effective_seconds").asDouble());
            row.set("statistics_completed_at", bucket.get("statistics_completed_at"));
            normalized.add(row);
        }
        return normalized;
    }

    /** 使用分组和时间范围归并同一行的两个执行口径。 */
    private void addMetric(Map<String, ObjectNode> items, JsonNode row) {
        String scope = row.path("metric_scope").asText();
        if ((!"EXECUTION".equals(scope) && !"UPSTREAM".equals(scope)) || !row.hasNonNull("bucket_start") || !row.hasNonNull("bucket_end")
                || !row.hasNonNull("group_key")) {
            throw new BusinessException(ResponseEnum.MONITOR_UPSTREAM_ERROR.getCode(), "监控指标行缺少必要字段", null);
        }
        String key = row.path("group_key").asText();
        String start = row.path("bucket_start").asText();
        String end = row.path("bucket_end").asText();
        ObjectNode item = items.computeIfAbsent(key + "\n" + start + "\n" + end,
                ignored -> newItem(key, start, end, row.path("effective_seconds").asDouble()));
        item.set("EXECUTION".equals(scope) ? "execution" : "upstream", normalizeMetric(row));
    }

    /** 将 CK 在 JSON 中可能以字符串编码的数字转换为页面数值。 */
    private ObjectNode normalizeMetric(JsonNode row) {
        ObjectNode metric = mapper.createObjectNode().put("average_qps", row.path("average_qps").asDouble()).put("success_tps",
                row.path("success_tps").asDouble());
        for (String field : COUNTS) {
            metric.put(field, row.path(field).asLong());
        }
        for (String field : RATES) {
            if (row.hasNonNull(field)) {
                metric.put(field, row.path(field).asDouble());
            } else {
                metric.putNull(field);
            }
        }
        return metric;
    }

    /** 已发布但没有请求的桶仍展示零流量。 */
    private void addCoveredBucket(Map<String, ObjectNode> items, JsonNode bucket) {
        if (bucket.hasNonNull("available_from") && bucket.hasNonNull("available_through")) {
            String start = bucket.path("available_from").asText();
            String end = bucket.path("available_through").asText();
            items.computeIfAbsent("\n" + start + "\n" + end, ignored -> newItem("", start, end, bucket.path("effective_seconds").asDouble()));
        }
    }

    /** 创建供视图使用的分组区间。 */
    private ObjectNode newItem(String key, String start, String end, double seconds) {
        return mapper.createObjectNode().put("key", key).put("bucketStart", start).put("bucketEnd", end).put("effectiveSeconds", seconds);
    }

    /** 没有对应口径时，仅将计数和流量补零。 */
    private void fillScope(ObjectNode item, String scope) {
        if (item.has(scope)) {
            return;
        }
        ObjectNode empty = mapper.createObjectNode().put("average_qps", 0).put("success_tps", 0);
        for (String field : COUNTS) {
            empty.put(field, 0);
        }
        empty.putNull("error_rate").putNull("timeout_rate").putNull("cache_hit_rate").putNull("p95_millis").putNull("p99_millis");
        item.set(scope, empty);
    }

    /** 通过现有业务库一次补齐当前分组的名称。 */
    private Map<String, String> loadNames(String group, Map<String, ObjectNode> items) {
        Map<String, String> names = Maps.newHashMap();
        Set<String> ids = Sets.newHashSet();
        for (ObjectNode item : items.values()) {
            String key = item.path("key").asText();
            if (!key.isEmpty()) {
                ids.add(key);
            }
        }
        if (ids.isEmpty() || "total".equals(group)) {
            return names;
        }
        return data.names(group, ids);
    }

    /** 仅对页面返回名称，无法关联时保留可读状态。 */
    private String displayName(String group, String key, Map<String, String> names) {
        if ("total".equals(group)) {
            return "汇总";
        }
        if ("interface".equals(group)) {
            return names.getOrDefault(key, "接口名称不可用");
        }
        return key.isEmpty() ? "未归属" : names.getOrDefault(key, "专家名称不可用");
    }

    /** 将日期边界转换为 CK 协议规定的北京时间。 */
    private static String midnight(LocalDate date) {
        return TIME.format(date.atStartOfDay(BEIJING));
    }

    /** 空字符串表示该筛选维度不限。 */
    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }
}
