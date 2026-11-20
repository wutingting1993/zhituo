package com.cmbchina.infrastructure.monitor.supportimpl;

import com.cmbchina.domain.monitor.support.MonitorDataSupport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
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
    private static final ZoneOffset BEIJING = ZoneOffset.ofHours(8);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSXXX");
    private static final List<Traffic> TRAFFIC = ImmutableList.of(new Traffic("sample-assets", "客户资产查询", "sample-sales", "客户经营专家", 3700, 42, 166),
            new Traffic("sample-assets", "客户资产查询", "sample-wealth", "财富顾问专家", 2150, 30, 194),
            new Traffic("sample-assets", "客户资产查询", "", "未归属", 330, 38, 185),
            new Traffic("sample-risk", "客户风险等级", "sample-sales", "客户经营专家", 1560, 70, 98),
            new Traffic("sample-risk", "客户风险等级", "sample-risk-team", "风险识别专家", 1310, 67, 114),
            new Traffic("sample-holdings", "客户持仓查询", "sample-wealth", "财富顾问专家", 1450, 100, 220),
            new Traffic("sample-holdings", "客户持仓查询", "sample-risk-team", "风险识别专家", 510, 100, 242));

    private final AtomicReference<OffsetDateTime> lastRefresh = new AtomicReference<>();

    @Autowired
    private ObjectMapper mapper;

    /** 记录本地模拟统计的更新时间。 */
    @Override
    public JsonNode refresh(JsonNode request) {
        lastRefresh.set(LocalDateTime.now().atOffset(BEIJING));
        return mapper.createArrayNode();
    }

    /** 模拟统计查询。 */
    @Override
    public ArrayNode queryStatistics(JsonNode request) {
        return query(request, false);
    }

    /** 模拟离线覆盖查询。 */
    @Override
    public ArrayNode queryCoverage(JsonNode request) {
        return query(request, true);
    }

    /** 生成指定查询类型的模拟数据。 */
    private ArrayNode query(JsonNode request, boolean coverageQuery) {
        OffsetDateTime start = OffsetDateTime.parse(request.path("start_time").asText());
        OffsetDateTime requestedEnd = OffsetDateTime.parse(request.path("end_time").asText());
        OffsetDateTime now = LocalDateTime.now().atOffset(BEIJING).truncatedTo(ChronoUnit.MINUTES);
        OffsetDateTime cutoff = now.minusMinutes(now.getMinute() % 10);
        OffsetDateTime end = requestedEnd.isBefore(cutoff) ? requestedEnd : cutoff;
        int bucketSeconds = request.path("bucket_seconds").asInt();
        ArrayNode result = mapper.createArrayNode();
        for (OffsetDateTime cursor = start; cursor.isBefore(end);) {
            OffsetDateTime boundary = bucketSeconds == 0 ? end : cursor.plusSeconds(bucketSeconds);
            OffsetDateTime bucketEnd = boundary.isBefore(end) ? boundary : end;
            if (coverageQuery) {
                result.add(coverage(cursor, bucketEnd, cutoff));
            } else {
                appendStatistics(result, request, cursor, bucketEnd);
            }
            cursor = bucketEnd;
        }
        return result;
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
    private ObjectNode coverage(OffsetDateTime start, OffsetDateTime end, OffsetDateTime cutoff) {
        OffsetDateTime refreshed = lastRefresh.get();
        OffsetDateTime completed = refreshed != null && refreshed.isAfter(cutoff) ? refreshed : cutoff;
        return mapper.createObjectNode().put("available_from", TIME.format(start)).put("available_through", TIME.format(end))
                .put("effective_seconds", ChronoUnit.SECONDS.between(start, end)).put("statistics_completed_at", TIME.format(completed));
    }

    /** 汇总同一桶内满足筛选条件的固定调用组合。 */
    private void appendStatistics(ArrayNode result, JsonNode query, OffsetDateTime start, OffsetDateTime end) {
        Map<String, Bucket> buckets = Maps.newLinkedHashMap();
        String group = query.path("group_by").asText();
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
    private boolean matches(JsonNode query, Traffic traffic) {
        String interfaceId = query.path("interface_id").asText();
        String agentId = query.path("agent_id").asText();
        boolean attributed = query.path("unattributed_only").asInt() == 0 || traffic.agentId.isEmpty();
        return (interfaceId.isEmpty() || interfaceId.equals(traffic.interfaceId)) && (agentId.isEmpty() || agentId.equals(traffic.agentId))
                && attributed;
    }

    /** 输出与配置服务消费的字段名和可空口径一致。 */
    private ObjectNode metric(String key, String scope, Bucket bucket, OffsetDateTime start, OffsetDateTime end) {
        boolean execution = EXECUTION.equals(scope);
        long seconds = ChronoUnit.SECONDS.between(start, end);
        long started = execution ? bucket.execution : bucket.upstream;
        long failed = execution ? bucket.executionFailures : bucket.upstreamFailures;
        long timeout = failed / 4;
        ObjectNode row = mapper.createObjectNode().put("group_key", key).put("metric_scope", scope).put("bucket_start", TIME.format(start))
                .put("bucket_end", TIME.format(end)).put("effective_seconds", seconds).put("started_count", started)
                .put("latency_sample_count", started).put("success_count", started - failed).put("failure_count", failed)
                .put("timeout_count", timeout).put("cancelled_count", 0).put("cache_read_count", execution ? bucket.cacheReads : 0)
                .put("cache_hit_count", execution ? bucket.cacheHits : 0).put("expert_count", execution ? bucket.experts.size() : 0)
                .put("interface_count", execution ? bucket.interfaces.size() : 0).put("average_qps", (double) started / seconds)
                .put("success_tps", (double) (started - failed) / seconds);
        putRatio(row, "error_rate", failed, started);
        putRatio(row, "timeout_rate", timeout, started);
        putRatio(row, "cache_hit_rate", bucket.cacheHits, execution ? bucket.cacheReads : 0);
        if (started == 0) {
            return row.putNull("p95_millis").putNull("p99_millis");
        }
        TreeMap<Integer, Long> samples = execution ? bucket.executionLatencies : bucket.upstreamLatencies;
        return row.put("p95_millis", percentile(samples, started, 0.95)).put("p99_millis", percentile(samples, started, 0.99));
    }

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
    private void putRatio(ObjectNode row, String field, long value, long total) {
        if (total == 0) {
            row.putNull(field);
        } else {
            row.put(field, (double) value / total);
        }
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
        private void add(Traffic traffic, OffsetDateTime start, OffsetDateTime end) {
            for (long minute = start.toEpochSecond(); minute < end.toEpochSecond(); minute += 600) {
                long variation = Math.floorMod(minute / 600 * 13 + traffic.callsPerBucket, 49);
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
