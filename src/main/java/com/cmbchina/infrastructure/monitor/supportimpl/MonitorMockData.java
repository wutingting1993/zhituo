package com.cmbchina.infrastructure.monitor.supportimpl;

import com.cmbchina.domain.monitor.model.MonitorCoverage;
import com.cmbchina.domain.monitor.model.MonitorFilter;
import com.cmbchina.domain.monitor.model.MonitorMetric;
import com.cmbchina.domain.monitor.model.MonitorMetricRow;
import com.cmbchina.domain.monitor.model.MonitorRange;
import com.cmbchina.domain.monitor.model.MonitorRefreshCommand;
import com.cmbchina.domain.monitor.support.MonitorDataSupport;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;
import lombok.AllArgsConstructor;
import java.util.function.BiConsumer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** 本地联调使用的确定性监控数据，返回与 CK 一致的平铺结果。 */
@Component
@ConditionalOnProperty(name = "app.monitor.mock-enabled", havingValue = "true")
public class MonitorMockData implements MonitorDataSupport {
    private static final String INTERFACE = "interface";
    private static final String EXPERT = "expert";
    private static final String EXECUTION = "EXECUTION";
    private static final String UPSTREAM = "UPSTREAM";
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final List<Traffic> TRAFFIC = ImmutableList.of(new Traffic("sample-assets", "客户资产查询", "sample-sales", "客户经营专家", 3700, 42, 166),
            new Traffic("sample-assets", "客户资产查询", "sample-wealth", "财富顾问专家", 2150, 30, 194),
            new Traffic("sample-assets", "客户资产查询", "", "未归属", 330, 38, 185),
            new Traffic("sample-risk", "客户风险等级", "sample-sales", "客户经营专家", 1560, 70, 98),
            new Traffic("sample-risk", "客户风险等级", "sample-risk-team", "风险识别专家", 1310, 67, 114),
            new Traffic("sample-holdings", "客户持仓查询", "sample-wealth", "财富顾问专家", 1450, 100, 220),
            new Traffic("sample-holdings", "客户持仓查询", "sample-risk-team", "风险识别专家", 510, 100, 242));

    private final AtomicReference<LocalDateTime> lastRefresh = new AtomicReference<>();

    /** 记录本地模拟统计的更新时间。 */
    @Override
    public void refresh(MonitorRefreshCommand request) {
        lastRefresh.set(LocalDateTime.now());
    }

    /** 模拟统计查询。 */
    @Override
    public List<MonitorMetricRow> queryStatistics(MonitorFilter request) {
        List<MonitorMetricRow> result = Lists.newArrayList();
        visitBuckets(request.getRange(), cutoff(), (start, end) -> appendStatistics(result, request, start, end));
        return result;
    }

    /** 模拟离线覆盖查询。 */
    @Override
    public List<MonitorCoverage> queryCoverage(MonitorRange request) {
        LocalDateTime cutoff = cutoff();
        List<MonitorCoverage> result = Lists.newArrayList();
        visitBuckets(request, cutoff, (start, end) -> result.add(coverage(start, end, cutoff)));
        return result;
    }

    /** 获取最近的已闭合桶边界。 */
    private LocalDateTime cutoff() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        return now.minusMinutes(now.getMinute() % 10);
    }

    /** 按相同的桶边界生成统计及覆盖数据。 */
    private void visitBuckets(MonitorRange request, LocalDateTime cutoff, BiConsumer<LocalDateTime, LocalDateTime> visitor) {
        LocalDateTime start = LocalDateTime.parse(request.getStartTime(), TIME);
        LocalDateTime requestedEnd = LocalDateTime.parse(request.getEndTime(), TIME);
        LocalDateTime end = requestedEnd.isBefore(cutoff) ? requestedEnd : cutoff;
        int bucketSeconds = request.getBucketSeconds();
        for (LocalDateTime cursor = start; cursor.isBefore(end);) {
            LocalDateTime boundary = bucketSeconds == 0 ? end : cursor.plusSeconds(bucketSeconds);
            LocalDateTime bucketEnd = boundary.isBefore(end) ? boundary : end;
            visitor.accept(cursor, bucketEnd);
            cursor = bucketEnd;
        }
    }

    /** 模拟标识只映射本地样例名称，无需在业务库写入虚拟专家。 */
    @Override
    public Map<String, String> names(String group, Set<String> ids) {
        Map<String, String> names = Maps.newHashMap();
        for (Traffic traffic : TRAFFIC) {
            if (INTERFACE.equals(group)) {
                names.put(traffic.interfaceId, traffic.interfaceName);
            } else if (EXPERT.equals(group) && !traffic.agentId.isEmpty()) {
                names.put(traffic.agentId, traffic.agentName);
            }
        }
        return names;
    }

    /** 空流量也可显示离线覆盖时间。 */
    private MonitorCoverage coverage(LocalDateTime start, LocalDateTime end, LocalDateTime cutoff) {
        LocalDateTime refreshed = lastRefresh.get();
        LocalDateTime completed = refreshed != null && refreshed.isAfter(cutoff) ? refreshed : cutoff;
        return MonitorCoverage.builder().availableFrom(TIME.format(start)).availableThrough(TIME.format(end))
                .effectiveSeconds(ChronoUnit.SECONDS.between(start, end)).statisticsCompletedAt(TIME.format(completed)).build();
    }

    /** 汇总同一桶内满足筛选条件的固定调用组合。 */
    private void appendStatistics(List<MonitorMetricRow> result, MonitorFilter query, LocalDateTime start, LocalDateTime end) {
        Map<String, Bucket> buckets = Maps.newLinkedHashMap();
        String group = query.getGroupBy();
        for (Traffic traffic : TRAFFIC) {
            if (matches(query, traffic)) {
                String key = INTERFACE.equals(group) ? traffic.interfaceId : EXPERT.equals(group) ? traffic.agentId : "";
                buckets.computeIfAbsent(key, ignored -> new Bucket()).add(traffic, start, end);
            }
        }
        buckets.forEach((key, bucket) -> {
            result.add(metric(key, EXECUTION, bucket, start, end));
            if (bucket.upstream > 0) {
                result.add(metric(key, UPSTREAM, bucket, start, end));
            }
        });
    }

    /** 接口筛选和专家筛选可共同生效。 */
    private boolean matches(MonitorFilter query, Traffic traffic) {
        String interfaceId = query.getInterfaceId();
        String agentId = query.getAgentId();
        boolean attributed = !query.isUnattributedOnly() || traffic.agentId.isEmpty();
        return (interfaceId.isEmpty() || interfaceId.equals(traffic.interfaceId)) && (agentId.isEmpty() || agentId.equals(traffic.agentId))
                && attributed;
    }

    /** 输出与配置服务消费的字段名和可空口径一致。 */
    private MonitorMetricRow metric(String key, String scope, Bucket bucket, LocalDateTime start, LocalDateTime end) {
        boolean execution = EXECUTION.equals(scope);
        long seconds = ChronoUnit.SECONDS.between(start, end);
        long started = execution ? bucket.execution : bucket.upstream;
        long failed = execution ? bucket.executionFailures : bucket.upstreamFailures;
        long timeout = failed / 4;
        TreeMap<Integer, Long> samples = execution ? bucket.executionLatencies : bucket.upstreamLatencies;
        MonitorMetric values = MonitorMetric.builder().startedCount(started).latencySampleCount(started).successCount(started - failed)
                .failureCount(failed).timeoutCount(timeout).cacheReadCount(execution ? bucket.cacheReads : 0)
                .cacheHitCount(execution ? bucket.cacheHits : 0).expertCount(execution ? bucket.experts.size() : 0)
                .interfaceCount(execution ? bucket.interfaces.size() : 0).averageQps((double) started / seconds)
                .successTps((double) (started - failed) / seconds).errorRate(ratio(failed, started)).timeoutRate(ratio(timeout, started))
                .cacheHitRate(ratio(bucket.cacheHits, execution ? bucket.cacheReads : 0)).p95Millis(nullablePercentile(samples, started, 0.95))
                .p99Millis(nullablePercentile(samples, started, 0.99)).build();
        return MonitorMetricRow.builder().groupKey(key).metricScope(scope).bucketStart(TIME.format(start)).bucketEnd(TIME.format(end))
                .effectiveSeconds(seconds).metric(values).build();
    }

    /** 无耗时样本时保留空值。 */
    private Double nullablePercentile(TreeMap<Integer, Long> samples, long count, double quantile) {
        return count == 0 ? null : (double) percentile(samples, count, quantile);
    }

    /** 查找给定分位的耗时。 */
    private int percentile(TreeMap<Integer, Long> samples, long count, double quantile) {
        long remaining = (long) Math.ceil(count * quantile);
        for (Map.Entry<Integer, Long> sample : samples.entrySet()) {
            remaining -= sample.getValue();
            if (remaining <= 0) {
                return sample.getKey();
            }
        }
        return samples.lastKey();
    }

    /** 没有分母时保留空值。 */
    private Double ratio(long value, long total) {
        return total == 0 ? null : (double) value / total;
    }

    /** 一条固定的专家与接口调用组合。 */
    @AllArgsConstructor
    private static final class Traffic {
        private final String interfaceId;
        private final String interfaceName;
        private final String agentId;
        private final String agentName;
        private final int callsPerBucket;
        private final int upstreamPercent;
        private final int latency;
    }

    /** 模拟计数以同一套 10 分钟明细累计，使列表、汇总和趋势能相互核对。 */
    private static final class Bucket {
        private static final LocalDateTime EPOCH = LocalDate.ofEpochDay(0).atStartOfDay();
        private long execution;
        private long upstream;
        private long executionFailures;
        private long upstreamFailures;
        private long cacheReads;
        private long cacheHits;
        private final TreeMap<Integer, Long> executionLatencies = new TreeMap<>();
        private final TreeMap<Integer, Long> upstreamLatencies = new TreeMap<>();
        private final Set<String> experts = Sets.newHashSet();
        private final Set<String> interfaces = Sets.newHashSet();

        /** 叠加所选时间内的离线模拟数据。 */
        private void add(Traffic traffic, LocalDateTime start, LocalDateTime end) {
            long endSecond = ChronoUnit.SECONDS.between(EPOCH, end);
            for (long second = ChronoUnit.SECONDS.between(EPOCH, start); second < endSecond; second += 600) {
                long variation = Math.floorMod(second / 600 * 13 + traffic.callsPerBucket, 49);
                long calls = traffic.callsPerBucket * (76 + variation) / 100;
                long sent = calls * traffic.upstreamPercent / 100;
                executionLatencies.merge(traffic.latency, calls, Long::sum);
                upstreamLatencies.merge(traffic.latency + 38, sent, Long::sum);
                execution += calls;
                upstream += sent;
                executionFailures += sent * 2 / 100;
                upstreamFailures += sent * 7 / 100;
                if (traffic.upstreamPercent < 100) {
                    cacheReads += calls;
                    cacheHits += calls - sent;
                }
            }
            if (!traffic.agentId.isEmpty()) {
                experts.add(traffic.agentId);
            }
            interfaces.add(traffic.interfaceId);
        }
    }
}
