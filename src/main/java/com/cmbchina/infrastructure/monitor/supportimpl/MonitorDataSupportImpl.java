package com.cmbchina.infrastructure.monitor.supportimpl;

import com.cmbchina.domain.monitor.model.MonitorCoverage;
import com.cmbchina.domain.monitor.model.MonitorFilter;
import com.cmbchina.domain.monitor.model.MonitorMetric;
import com.cmbchina.domain.monitor.model.MonitorMetricRow;
import com.cmbchina.domain.monitor.model.MonitorRange;
import com.cmbchina.domain.monitor.model.MonitorRefreshCommand;
import com.cmbchina.domain.monitor.support.MonitorDataSupport;
import com.cmbchina.infrastructure.common.ResponseEnum;
import com.cmbchina.infrastructure.common.exception.BusinessException;
import com.cmbchina.infrastructure.monitor.dao.mapper.MonitorNameMapper;
import com.cmbchina.infrastructure.monitor.dao.mapper.MonitorNameRecord;
import com.google.common.collect.Maps;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.util.stream.Collectors;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/** 读取 ClickHouse 离线统计，并从合并后的业务库补齐名称。 */
@Component
@ConditionalOnProperty(name = "app.monitor.mock-enabled", havingValue = "false", matchIfMissing = true)
public class MonitorDataSupportImpl implements MonitorDataSupport {

    @Value("${app.monitor.clickhouse-base-url:}")
    private String baseUrl;
    @Value("${app.monitor.clickhouse-statistics-path}")
    private String statisticsPath;
    @Value("${app.monitor.clickhouse-coverage-path}")
    private String coveragePath;
    @Value("${app.monitor.clickhouse-refresh-path}")
    private String refreshPath;
    @Autowired
    private RestTemplateBuilder builder;
    @Autowired
    private MonitorNameMapper nameMapper;

    private RestTemplate queryClient;
    private RestTemplate refreshClient;

    /** 分别初始化查询和手动重算的传输时限。 */
    @PostConstruct
    public void initialize() {
        RestTemplateBuilder base = builder.setConnectTimeout(Duration.ofSeconds(5));
        queryClient = base.setReadTimeout(Duration.ofSeconds(20)).build();
        refreshClient = base.setReadTimeout(Duration.ofMinutes(2)).build();
    }

    /** 查询平铺统计结果。 */
    @Override
    public List<MonitorMetricRow> queryStatistics(MonitorFilter request) {
        StatisticsRequest body = StatisticsRequest.builder().startTime(request.getRange().getStartTime()).endTime(request.getRange().getEndTime())
                .bucketSeconds(request.getRange().getBucketSeconds()).groupBy(request.getGroupBy()).interfaceId(request.getInterfaceId())
                .agentId(request.getAgentId()).unattributedOnly(request.isUnattributedOnly() ? 1 : 0).build();
        ClickHouseResponse<List<StatisticsRow>> response = invoke(queryClient, statisticsPath, body, statisticsResponse());
        return rows(response).stream().map(this::metricRow).collect(Collectors.toList());
    }

    /** 查询离线覆盖结果。 */
    @Override
    public List<MonitorCoverage> queryCoverage(MonitorRange request) {
        CoverageRequest body = CoverageRequest.builder().startTime(request.getStartTime()).endTime(request.getEndTime())
                .bucketSeconds(request.getBucketSeconds()).build();
        ClickHouseResponse<List<CoverageRow>> response = invoke(queryClient, coveragePath, body, coverageResponse());
        return rows(response).stream()
                .map(row -> MonitorCoverage.builder().availableFrom(row.availableFrom).availableThrough(row.availableThrough)
                        .effectiveSeconds(row.effectiveSeconds).statisticsCompletedAt(row.statisticsCompletedAt).build())
                .collect(Collectors.toList());
    }

    /** 转发离线更新。 */
    @Override
    public void refresh(MonitorRefreshCommand request) {
        RefreshRequest body = RefreshRequest.builder().periodType(request.getPeriodType()).offset(request.getOffset()).build();
        invoke(refreshClient, refreshPath, body, refreshResponse());
    }

    /** 将 ClickHouse 协议值转换为领域指标。 */
    private MonitorMetricRow metricRow(StatisticsRow row) {
        MonitorMetric metric = MonitorMetric.builder().startedCount(row.startedCount).successCount(row.successCount).failureCount(row.failureCount)
                .timeoutCount(row.timeoutCount).cancelledCount(row.cancelledCount).cacheReadCount(row.cacheReadCount).cacheHitCount(row.cacheHitCount)
                .expertCount(row.expertCount).interfaceCount(row.interfaceCount).latencySampleCount(row.latencySampleCount).averageQps(row.averageQps)
                .successTps(row.successTps).errorRate(row.errorRate).timeoutRate(row.timeoutRate).cacheHitRate(row.cacheHitRate)
                .p95Millis(row.p95Millis).p99Millis(row.p99Millis).build();
        return MonitorMetricRow.builder().groupKey(row.groupKey).metricScope(row.metricScope).bucketStart(row.bucketStart).bucketEnd(row.bucketEnd)
                .effectiveSeconds(row.effectiveSeconds).metric(metric).build();
    }

    /** 校验查询响应包含列表。 */
    private <T> List<T> rows(ClickHouseResponse<List<T>> response) {
        if (response.getData() == null || response.getData().contains(null)) {
            throw new BusinessException(ResponseEnum.MONITOR_UPSTREAM_ERROR.getCode(), "监控统计响应格式不正确", null);
        }
        return response.getData();
    }

    /** 从当前数据源批量读取名称。 */
    @Override
    public Map<String, String> names(String group, Set<String> ids) {
        List<MonitorNameRecord> records = "interface".equals(group) ? nameMapper.findInterfaces(ids) : nameMapper.findExperts(ids);
        Map<String, String> names = Maps.newHashMap();
        for (MonitorNameRecord record : records) {
            names.put(record.getId(), record.getName());
        }
        return names;
    }

    /** 统一解包 CK 响应，并保留传输异常。 */
    private <T> ClickHouseResponse<T> invoke(RestTemplate client, String path, Object request,
            ParameterizedTypeReference<ClickHouseResponse<T>> responseType) {
        if (baseUrl.trim().isEmpty() || path.trim().isEmpty()) {
            throw new BusinessException(ResponseEnum.MONITOR_UPSTREAM_ERROR.getCode(), "监控数据服务尚未配置", null);
        }
        String base = baseUrl.trim();
        String endpoint = path.trim();
        String prefix = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        String url = endpoint.startsWith("/") ? prefix + endpoint : prefix + "/" + endpoint;
        ClickHouseResponse<T> response;
        try {
            response = client.exchange(url, HttpMethod.POST, new HttpEntity<>(request), responseType).getBody();
        } catch (RestClientException error) {
            throw new BusinessException(ResponseEnum.MONITOR_UPSTREAM_ERROR.getCode(), "监控数据服务暂不可用", error);
        }
        if (response == null || !response.isSuccess()) {
            throw new BusinessException(ResponseEnum.MONITOR_UPSTREAM_ERROR.getCode(), "监控数据服务处理失败", null);
        }
        return response;
    }

    /** 统计查询的 CK 参数。 */
    @Getter
    @Builder
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    private static final class StatisticsRequest {
        private String startTime;
        private String endTime;
        private int bucketSeconds;
        private String groupBy;
        private String interfaceId;
        private String agentId;
        private int unattributedOnly;
    }

    /** 覆盖查询的 CK 参数。 */
    @Getter
    @Builder
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    private static final class CoverageRequest {
        private String startTime;
        private String endTime;
        private int bucketSeconds;
    }

    /** 刷新接口的 CK 参数。 */
    @Getter
    @Builder
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    private static final class RefreshRequest {
        private String periodType;
        private long offset;
    }

    /** CK 统计结果的一行。 */
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    private static final class StatisticsRow {
        private String groupKey;
        private String metricScope;
        private String bucketStart;
        private String bucketEnd;
        private double effectiveSeconds;
        private long startedCount;
        private long successCount;
        private long failureCount;
        private long timeoutCount;
        private long cancelledCount;
        private long cacheReadCount;
        private long cacheHitCount;
        private long expertCount;
        private long interfaceCount;
        private long latencySampleCount;
        private double averageQps;
        private double successTps;
        private Double errorRate;
        private Double timeoutRate;
        private Double cacheHitRate;
        private Double p95Millis;
        private Double p99Millis;
    }

    /** CK 覆盖结果的一行。 */
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    private static final class CoverageRow {
        private String availableFrom;
        private String availableThrough;
        private double effectiveSeconds;
        private String statisticsCompletedAt;
    }

    /** 声明 CK 响应的具体反序列化类型。 */
    private static ParameterizedTypeReference<ClickHouseResponse<List<StatisticsRow>>> statisticsResponse() {
        return new ParameterizedTypeReference<ClickHouseResponse<List<StatisticsRow>>>() {
        };
    }

    /** 声明 CK 响应的具体反序列化类型。 */
    private static ParameterizedTypeReference<ClickHouseResponse<List<CoverageRow>>> coverageResponse() {
        return new ParameterizedTypeReference<ClickHouseResponse<List<CoverageRow>>>() {
        };
    }

    /** 声明 CK 响应的具体反序列化类型。 */
    private static ParameterizedTypeReference<ClickHouseResponse<Void>> refreshResponse() {
        return new ParameterizedTypeReference<ClickHouseResponse<Void>>() {
        };
    }

    /** CK 统一响应信封。 */
    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static final class ClickHouseResponse<T> {
        private boolean success;
        private T data;
    }
}
