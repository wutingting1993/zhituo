package com.cmbchina.application.monitor.service;

import com.cmbchina.application.monitor.api.MonitorApplicationService;
import com.cmbchina.application.monitor.api.dto.MonitorQuery;
import com.cmbchina.application.monitor.api.dto.MonitorBatchResponse;
import com.cmbchina.application.monitor.api.dto.MonitorBatchMetricResponse;
import com.cmbchina.domain.monitor.model.MonitorBatchMetric;
import com.cmbchina.application.monitor.api.dto.MonitorDistributionResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import com.cmbchina.application.monitor.api.dto.MonitorRefresh;
import com.cmbchina.application.monitor.api.dto.MonitorCoverageResponse;
import com.cmbchina.application.monitor.api.dto.MonitorItemResponse;
import com.cmbchina.application.monitor.api.dto.MonitorMetricsResponse;
import com.cmbchina.application.monitor.api.dto.MonitorOverviewResponse;
import com.cmbchina.application.monitor.api.dto.MonitorQueryResponse;
import com.cmbchina.domain.monitor.model.MonitorCoverage;
import com.cmbchina.domain.monitor.model.MonitorFilter;
import com.cmbchina.domain.monitor.model.MonitorMetric;
import com.cmbchina.domain.monitor.model.MonitorMetricRow;
import com.cmbchina.domain.monitor.model.MonitorRange;
import com.cmbchina.domain.monitor.model.MonitorRefreshCommand;
import com.cmbchina.domain.monitor.support.MonitorDataSupport;
import com.cmbchina.infrastructure.common.ResponseEnum;
import com.cmbchina.infrastructure.common.exception.BusinessException;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** 校验监控查询，聚合同一统计桶并补全业务名称。 */
@Service
public class InterfaceMonitorService implements MonitorApplicationService {
    private static final int MINUTE_SECONDS = 60;
    private static final int RANK_LIMIT = 10;
    private static final int LOOKBACK_MONTHS = 6;
    private static final int COVERAGE_BUCKET_SECONDS = 600;
    private static final long MAX_OFFSET = 4294967295L;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final int DAY_BUCKET_SECONDS = 86400;
    private static final Set<String> PERIODS = ImmutableSet.of("day", "week", "month");
    @Autowired
    private MonitorDataSupport data;

    /** 查询全局总览。 */
    @Override
    public MonitorQueryResponse overview(MonitorQuery request) {
        return query(request, "total", 0, true);
    }

    /** 查询接口指标列表。 */
    @Override
    public MonitorQueryResponse interfaces(MonitorQuery request) {
        return query(request, "interface", 0, false);
    }

    /** 查询专家指标列表。 */
    @Override
    public MonitorQueryResponse experts(MonitorQuery request) {
        return query(request, "expert", 0, false);
    }

    /** 查询详情汇总。 */
    @Override
    public MonitorQueryResponse summary(MonitorQuery request) {
        return query(request, "total", 0, false);
    }

    /** 查询详情趋势。 */
    @Override
    public MonitorQueryResponse trend(MonitorQuery request) {
        int bucketSeconds = Objects.equals(request.getStartDate(), request.getEndDate()) ? COVERAGE_BUCKET_SECONDS : DAY_BUCKET_SECONDS;
        return query(request, "total", bucketSeconds, false);
    }

    /** 按入口确定的统计口径读取并组装监控数据。 */
    private MonitorQueryResponse query(MonitorQuery request, String group, int bucketSeconds, boolean includeOverview) {
        validate(request);
        MonitorRange range = MonitorRange.builder().startTime(midnight(request.getStartDate())).endTime(midnight(request.getEndDate().plusDays(1)))
                .bucketSeconds(bucketSeconds).build();
        MonitorFilter filters = MonitorFilter.builder().range(range).groupBy(group).interfaceId(orEmpty(request.getInterfaceId()))
                .agentId(orEmpty(request.getAgentId())).unattributedOnly(request.isUnattributedOnly()).build();
        List<MonitorMetricRow> rows = data.queryStatistics(filters);
        List<MonitorCoverage> coverage = data.queryCoverage(range);
        MonitorQueryResponse response = assemble(group, bucketSeconds, rows, coverage);
        if (includeOverview) {
            MonitorMetricsResponse total = response.getItems().isEmpty()
                    ? MonitorMetricsResponse.builder().build()
                    : response.getItems().get(0).getExecution();
            response.setOverview(overview(total, filters));
        }
        return response;
    }

    /** 汇总全局统计并生成三个排行榜。 */
    private MonitorOverviewResponse overview(MonitorMetricsResponse total, MonitorFilter filters) {
        List<MonitorItemResponse> interfaces = overviewRows(filters, "interface");
        List<MonitorItemResponse> experts = overviewRows(filters, "expert");
        long completed = total.getSuccessCount() + total.getFailureCount() + total.getCancelledCount();
        List<MonitorDistributionResponse> distribution = Lists.newArrayList(distribution("成功", total.getSuccessCount(), completed),
                distribution("其他失败", total.getFailureCount() - total.getTimeoutCount(), completed),
                distribution("超时", total.getTimeoutCount(), completed), distribution("取消", total.getCancelledCount(), completed));
        return MonitorOverviewResponse.builder().executionCount(total.getStartedCount()).completedCount(completed).resultDistribution(distribution)
                .failureCount(total.getFailureCount()).affectedInterfaceCount(affectedCount(interfaces)).affectedExpertCount(affectedCount(experts))
                .errorRate(total.getErrorRate()).p95Millis(total.getP95Millis())
                .interfaceFailureTop(rank(interfaces, MonitorMetricsResponse::getFailureCount, metric -> metric.getFailureCount() > 0))
                .interfaceLatencyTop(rank(interfaces, metric -> metric.getP95Millis() == null ? 0 : metric.getP95Millis(),
                        metric -> metric.getLatencySampleCount() > 0))
                .expertFailureTop(rank(experts, MonitorMetricsResponse::getFailureCount, metric -> metric.getFailureCount() > 0)).build();
    }

    /** 百分比统一保留两位小数，无已结束调用时为空。 */
    private MonitorDistributionResponse distribution(String name, long count, long completed) {
        Double percentage = completed == 0
                ? null
                : BigDecimal.valueOf(count).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(completed), 2, RoundingMode.HALF_UP)
                        .doubleValue();
        return MonitorDistributionResponse.builder().name(name).count(count).percentage(percentage).build();
    }

    /** 读取榜单候选行并补充名称。 */
    private List<MonitorItemResponse> overviewRows(MonitorFilter filters, String group) {
        MonitorFilter grouped = MonitorFilter.builder().range(filters.getRange()).groupBy(group).interfaceId(filters.getInterfaceId())
                .agentId(filters.getAgentId()).unattributedOnly(filters.isUnattributedOnly()).build();
        Map<String, MonitorItemResponse> items = Maps.newLinkedHashMap();
        for (MonitorMetricRow row : data.queryStatistics(grouped)) {
            if ("EXECUTION".equals(row.getMetricScope()) && row.getGroupKey() != null && !row.getGroupKey().isEmpty()) {
                addMetric(items, row);
            }
        }
        Map<String, String> names = loadNames(group, items);
        items.values().forEach(item -> item.setName(displayName(group, item.getKey(), names)));
        return Lists.newArrayList(items.values());
    }

    /** 统计有执行失败的接口或专家数量。 */
    private long affectedCount(List<MonitorItemResponse> items) {
        return items.stream().filter(item -> item.getExecution().getFailureCount() > 0).count();
    }

    /** 按指标降序取榜单，同分时按名称和标识排序。 */
    private List<MonitorItemResponse> rank(List<MonitorItemResponse> items, ToDoubleFunction<MonitorMetricsResponse> metric,
            Predicate<MonitorMetricsResponse> eligibility) {
        return items.stream().filter(item -> eligibility.test(item.getExecution()))
                .sorted(Comparator.<MonitorItemResponse>comparingDouble(item -> metric.applyAsDouble(item.getExecution())).reversed()
                        .thenComparing(MonitorItemResponse::getName).thenComparing(MonitorItemResponse::getKey))
                .limit(RANK_LIMIT).collect(Collectors.toList());
    }

    /** 按专家和日期查询批量入口指标，接口筛选由内部接口表格单独处理。 */
    @Override
    public MonitorBatchResponse batchMetrics(MonitorQuery request) {
        validate(request);
        MonitorRange range = MonitorRange.builder().startTime(midnight(request.getStartDate())).endTime(midnight(request.getEndDate().plusDays(1)))
                .bucketSeconds(MINUTE_SECONDS).build();
        MonitorFilter minutes = MonitorFilter.builder().range(range).groupBy("minute").interfaceId("").agentId(orEmpty(request.getAgentId()))
                .unattributedOnly(request.isUnattributedOnly()).build();
        MonitorFilter total = MonitorFilter.builder().range(range).groupBy("total").interfaceId("").agentId(minutes.getAgentId())
                .unattributedOnly(minutes.isUnattributedOnly()).build();
        List<MonitorBatchMetric> summaries = data.queryBatchStatistics(total);
        List<MonitorBatchMetric> points = data.queryBatchStatistics(minutes);
        List<MonitorCoverageResponse> coverage = points.stream().map(this::batchCoverage).collect(Collectors.toList());
        MonitorBatchMetric summary = summaries.isEmpty() ? null : summaries.get(0);
        return MonitorBatchResponse.builder().summary(summary == null ? null : batchMetric(summary))
                .points(points.stream().map(this::batchMetric).collect(Collectors.toList())).coverage(coverage)
                .statisticsCompletedAt(summary == null ? null : summary.getStatisticsCompletedAt()).bucketSeconds(MINUTE_SECONDS).build();
    }

    /** 输出与分钟数据对应的覆盖边界。 */
    private MonitorCoverageResponse batchCoverage(MonitorBatchMetric metric) {
        return MonitorCoverageResponse.builder().availableFrom(metric.getBucketStart()).availableThrough(metric.getBucketEnd())
                .effectiveSeconds(metric.getEffectiveSeconds()).statisticsCompletedAt(metric.getStatisticsCompletedAt()).build();
    }

    /** 将已由数据库聚合的批量请求指标转换为接口字段。 */
    private MonitorBatchMetricResponse batchMetric(MonitorBatchMetric metric) {
        return MonitorBatchMetricResponse.builder().startedCount(metric.getStartedCount()).successCount(metric.getSuccessCount())
                .partialFailureCount(metric.getPartialFailureCount()).failureCount(metric.getFailureCount())
                .cancelledCount(metric.getCancelledCount()).completedCount(metric.getCompletedCount())
                .latencySampleCount(metric.getLatencySampleCount()).averageMillis(metric.getAverageMillis()).p95Millis(metric.getP95Millis())
                .qps(metric.getQps()).completedTps(metric.getCompletedTps()).successTps(metric.getSuccessTps())
                .partialFailureRate(metric.getPartialFailureRate()).failureRate(metric.getFailureRate()).bucketStart(metric.getBucketStart())
                .bucketEnd(metric.getBucketEnd()).build();
    }

    /** 手动重算指定周期，无页面回看窗口限制。 */
    @Override
    public void refresh(MonitorRefresh request) {
        if (!PERIODS.contains(request.getPeriodType()) || request.getOffset() < 0 || request.getOffset() > MAX_OFFSET) {
            throw new BusinessException(ResponseEnum.VALIDATION_ERROR.getCode(), "离线更新周期或偏移量无效", null);
        }
        data.refresh(MonitorRefreshCommand.builder().periodType(request.getPeriodType()).offset(request.getOffset()).build());
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
    private MonitorQueryResponse assemble(String group, int bucketSeconds, List<MonitorMetricRow> rows, List<MonitorCoverage> coverage) {
        Map<String, MonitorItemResponse> items = Maps.newLinkedHashMap();
        for (MonitorMetricRow row : rows) {
            addMetric(items, row);
        }
        if (bucketSeconds > 0 && "total".equals(group)) {
            for (MonitorCoverage bucket : coverage) {
                addCoveredBucket(items, bucket);
            }
        }
        Map<String, String> names = loadNames(group, items);
        for (MonitorItemResponse item : items.values()) {
            item.setName(displayName(group, item.getKey(), names));
            if (item.getExecution() == null) {
                item.setExecution(MonitorMetricsResponse.builder().build());
            }
            if (item.getUpstream() == null) {
                item.setUpstream(MonitorMetricsResponse.builder().build());
            }
        }
        return MonitorQueryResponse.builder().items(Lists.newArrayList(items.values()))
                .coverage(coverage.stream()
                        .map(bucket -> MonitorCoverageResponse.builder().availableFrom(bucket.getAvailableFrom())
                                .availableThrough(bucket.getAvailableThrough()).effectiveSeconds(bucket.getEffectiveSeconds())
                                .statisticsCompletedAt(bucket.getStatisticsCompletedAt()).build())
                        .collect(Collectors.toList()))
                .statisticsCompletedAt(latestCompletedAt(coverage)).build();
    }

    /** 获取最近一次离线统计完成时间。 */
    private String latestCompletedAt(List<MonitorCoverage> coverage) {
        return coverage.stream().map(MonitorCoverage::getStatisticsCompletedAt).filter(Objects::nonNull).filter(value -> !value.isEmpty())
                .max(Comparator.naturalOrder()).orElse(null);
    }

    /** 使用分组和时间范围归并同一行的两个执行口径。 */
    private void addMetric(Map<String, MonitorItemResponse> items, MonitorMetricRow row) {
        String scope = row.getMetricScope();
        String key = row.getGroupKey();
        String start = row.getBucketStart();
        String end = row.getBucketEnd();
        if ((!"EXECUTION".equals(scope) && !"UPSTREAM".equals(scope)) || start == null || end == null || key == null) {
            throw new BusinessException(ResponseEnum.MONITOR_UPSTREAM_ERROR.getCode(), "监控指标行缺少必要字段", null);
        }
        MonitorItemResponse item = items.computeIfAbsent(key + "\n" + start + "\n" + end,
                ignored -> newItem(key, start, end, row.getEffectiveSeconds()));
        if ("EXECUTION".equals(scope)) {
            item.setExecution(metricResponse(row.getMetric()));
        } else {
            item.setUpstream(metricResponse(row.getMetric()));
        }
    }

    /** 将领域统计值映射到页面指标。 */
    private MonitorMetricsResponse metricResponse(MonitorMetric metric) {
        return MonitorMetricsResponse.builder().startedCount(metric.getStartedCount()).successCount(metric.getSuccessCount())
                .failureCount(metric.getFailureCount()).timeoutCount(metric.getTimeoutCount()).cancelledCount(metric.getCancelledCount())
                .cacheReadCount(metric.getCacheReadCount()).cacheHitCount(metric.getCacheHitCount()).expertCount(metric.getExpertCount())
                .interfaceCount(metric.getInterfaceCount()).latencySampleCount(metric.getLatencySampleCount()).averageQps(metric.getAverageQps())
                .successTps(metric.getSuccessTps()).activeSecondP95Qps(metric.getActiveSecondP95Qps())
                .activeSecondP95SuccessTps(metric.getActiveSecondP95SuccessTps()).peakQps(metric.getPeakQps()).errorRate(metric.getErrorRate())
                .timeoutRate(metric.getTimeoutRate()).cacheHitRate(metric.getCacheHitRate()).p95Millis(metric.getP95Millis())
                .p99Millis(metric.getP99Millis()).build();
    }

    /** 已发布但没有请求的桶仍展示零流量。 */
    private void addCoveredBucket(Map<String, MonitorItemResponse> items, MonitorCoverage bucket) {
        String start = bucket.getAvailableFrom();
        String end = bucket.getAvailableThrough();
        if (start != null && end != null) {
            items.computeIfAbsent("\n" + start + "\n" + end, ignored -> newItem("", start, end, bucket.getEffectiveSeconds()));
        }
    }

    /** 创建供视图使用的分组区间。 */
    private MonitorItemResponse newItem(String key, String start, String end, double seconds) {
        return MonitorItemResponse.builder().key(key).bucketStart(start).bucketEnd(end).effectiveSeconds(seconds).build();
    }

    /** 通过现有业务库一次补齐当前分组的名称。 */
    private Map<String, String> loadNames(String group, Map<String, MonitorItemResponse> items) {
        Set<String> ids = Sets.newHashSet();
        for (MonitorItemResponse item : items.values()) {
            if (!item.getKey().isEmpty()) {
                ids.add(item.getKey());
            }
        }
        if (ids.isEmpty() || "total".equals(group)) {
            return Maps.newHashMap();
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

    /** 将日期边界转换为 监控查询使用的本地时间。 */
    private static String midnight(LocalDate date) {
        return TIME.format(date.atStartOfDay());
    }

    /** 空字符串表示该筛选维度不限。 */
    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }
}
