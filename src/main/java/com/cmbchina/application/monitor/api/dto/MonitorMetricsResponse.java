package com.cmbchina.application.monitor.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Getter;

/** 页面展示的一组调用指标。 */
@Getter
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class MonitorMetricsResponse {
    private final long startedCount;
    private final long successCount;
    private final long failureCount;
    private final long timeoutCount;
    private final long cancelledCount;
    private final long cacheReadCount;
    private final long cacheHitCount;
    private final long expertCount;
    private final long interfaceCount;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final Long latencySampleCount;
    private final double averageQps;
    private final double successTps;
    private final Long activeSecondP95Qps;
    private final Long activeSecondP95SuccessTps;
    private final Long peakQps;
    private final Double errorRate;
    private final Double timeoutRate;
    private final Double cacheHitRate;
    private final Double p95Millis;
    private final Double p99Millis;
}
