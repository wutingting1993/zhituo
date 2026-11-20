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
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.StreamSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** 校验监控查询，聚合同一统计桶并补全业务名称。 */
@Service
public class InterfaceMonitorService implements MonitorApplicationService {
    private static final ZoneId BEIJING = ZoneId.of("Asia/Shanghai");
    private static final int RANK_LIMIT = 10;
    private static final int LOOKBACK_MONTHS = 6;
    private static final int COVERAGE_BUCKET_SECONDS = 600;
    private static final long MAX_OFFSET = 4294967295L;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSXXX");
    private static final int DAY_BUCKET_SECONDS = 86400;
    private static final Set<String> PERIODS = ImmutableSet.of("day", "week", "month");
    private static final String[] RATES = {"error_rate", "timeout_rate", "cache_hit_rate", "p95_millis", "p99_millis"};
    private static final String[] COUNTS = {"started_count", "success_count", "failure_count", "timeout_count", "cancelled_count", "cache_read_count",
            "cache_hit_count", "expert_count", "interface_count"};

    @Autowired
    private MonitorDataSupport data;

    @Autowired
    private ObjectMapper mapper;

    /** 查询全局总览。 */
    @Override
    public JsonNode overview(MonitorQuery request) {
        return query(request, "total", 0, true);
    }

    /** 查询接口指标列表。 */
    @Override
    public JsonNode interfaces(MonitorQuery request) {
        return query(request, "interface", 0, false);
    }

    /** 查询专家指标列表。 */
    @Override
    public JsonNode experts(MonitorQuery request) {
        return query(request, "expert", 0, false);
    }

    /** 查询详情汇总。 */
    @Override
    public JsonNode summary(MonitorQuery request) {
        return query(request, "total", 0, false);
    }

    /** 查询详情趋势。 */
    @Override
    public JsonNode trend(MonitorQuery request) {
        int bucketSeconds = Objects.equals(request.getStartDate(), request.getEndDate()) ? COVERAGE_BUCKET_SECONDS : DAY_BUCKET_SECONDS;
        return query(request, "total", bucketSeconds, false);
    }

    /** 按入口确定的统计口径读取并组装监控数据。 */
    private JsonNode query(MonitorQuery request, String group, int bucketSeconds, boolean includeOverview) {
        validate(request);
        ObjectNode range = mapper.createObjectNode().put("start_time", midnight(request.getStartDate()))
                .put("end_time", midnight(request.getEndDate().plusDays(1))).put("bucket_seconds", bucketSeconds);
        ObjectNode filters = range.deepCopy().put("group_by", group).put("interface_id", orEmpty(request.getInterfaceId()))
                .put("agent_id", orEmpty(request.getAgentId())).put("unattributed_only", request.isUnattributedOnly() ? 1 : 0);
        ArrayNode rows = data.queryStatistics(filters);
        ArrayNode coverage = data.queryCoverage(range);
        ObjectNode response = assemble(group, bucketSeconds, rows, coverage);
        if (includeOverview) {
            response.set("overview", overview(response.path("items").path(0).path("execution"), filters));
        }
        return response;
    }

    private ObjectNode overview(JsonNode total, ObjectNode filters) {
        ArrayNode interfaces = overviewRows(filters, "interface");
        ArrayNode experts = overviewRows(filters, "expert");
        ObjectNode result = mapper.createObjectNode().put("executionCount", total.path("started_count").asLong())
                .put("failureCount", total.path("failure_count").asLong()).put("affectedInterfaceCount", affectedCount(interfaces))
                .put("affectedExpertCount", affectedCount(experts));
        result.set("errorRate", total.path("error_rate").isNumber() ? total.path("error_rate") : mapper.getNodeFactory().nullNode());
        return result.setAll(overviewRanks(total, interfaces, experts));
    }

    private ObjectNode overviewRanks(JsonNode total, ArrayNode interfaces, ArrayNode experts) {
        ObjectNode result = mapper.createObjectNode();
        result.set("p95Millis", total.path("p95_millis").isNumber() ? total.path("p95_millis") : mapper.getNodeFactory().nullNode());
        result.set("interfaceFailureTop", rank(interfaces, "failure_count", "failure_count"));
        result.set("interfaceLatencyTop", rank(interfaces, "p95_millis", "latency_sample_count"));
        result.set("expertFailureTop", rank(experts, "failure_count", "failure_count"));
        return result;
    }

    private ArrayNode overviewRows(ObjectNode filters, String group) {
        Map<String, ObjectNode> items = Maps.newLinkedHashMap();
        for (JsonNode row : data.queryStatistics(filters.deepCopy().put("group_by", group))) {
            if ("EXECUTION".equals(row.path("metric_scope").asText()) && !row.path("group_key").asText().isEmpty()) {
                addMetric(items, row);
            }
        }
        Map<String, String> names = loadNames(group, items);
        ArrayNode result = mapper.createArrayNode();
        for (ObjectNode item : items.values()) {
            item.put("name", displayName(group, item.path("key").asText(), names));
            result.add(item);
        }
        return result;
    }

    private long affectedCount(ArrayNode items) {
        return StreamSupport.stream(items.spliterator(), false).filter(item -> item.path("execution").path("failure_count").asLong() > 0).count();
    }

    private ArrayNode rank(ArrayNode items, String metric, String eligibility) {
        ArrayNode result = mapper.createArrayNode();
        StreamSupport.stream(items.spliterator(), false).filter(item -> item.path("execution").path(eligibility).asLong() > 0)
                .sorted(Comparator.<JsonNode>comparingDouble(item -> item.path("execution").path(metric).asDouble()).reversed()
                        .thenComparing(item -> item.path("name").asText()).thenComparing(item -> item.path("key").asText()))
                .limit(RANK_LIMIT).forEach(result::add);
        return result;
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
        if (request.isUnattributedOnly() && !orEmpty(request.getAgentId()).isEmpty()) {
            throw new BusinessException(ResponseEnum.VALIDATION_ERROR.getCode(), "未归属筛选不能同时选择领域专家", null);
        }
    }

    /** 校验北京时间下页面可查询的日期范围。 */
    private void validateDates(LocalDate start, LocalDate end) {
        LocalDate today = LocalDate.now();
        if (start == null || end == null || end.isBefore(start) || end.isAfter(today) || start.isBefore(today.minusMonths(LOOKBACK_MONTHS))) {
            throw new BusinessException(ResponseEnum.VALIDATION_ERROR.getCode(), "请选择最近六个月内的有效日期范围", null);
        }
    }

    /** 将两种口径和覆盖信息组装为页面使用的数据。 */
    private ObjectNode assemble(String group, int bucketSeconds, ArrayNode rows, ArrayNode coverage) {
        Map<String, ObjectNode> items = Maps.newLinkedHashMap();
        for (JsonNode row : rows) {
            addMetric(items, row);
        }
        if (bucketSeconds > 0 && "total".equals(group)) {
            for (JsonNode bucket : coverage) {
                addCoveredBucket(items, bucket);
            }
        }
        Map<String, String> names = loadNames(group, items);
        ArrayNode result = mapper.createArrayNode();
        for (ObjectNode item : items.values()) {
            String key = item.path("key").asText();
            item.put("name", displayName(group, key, names));
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
        metric.put("latency_sample_count", row.path("latency_sample_count").asLong());
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
