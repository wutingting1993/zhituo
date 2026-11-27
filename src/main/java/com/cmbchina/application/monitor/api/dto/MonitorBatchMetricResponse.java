package com.cmbchina.application.monitor.api.dto;

import lombok.Builder;
import lombok.Getter;

/** 展示专家批量请求的完整周期指标及自然分钟边界。 */
@Getter
@Builder
public class MonitorBatchMetricResponse {
    private final long startedCount;
    private final long successCount;
    private final long partialFailureCount;
    private final long failureCount;
    private final long cancelledCount;
    private final long completedCount;
    private final long latencySampleCount;
    private final Double averageMillis;
    private final Double p95Millis;
    private final double qps;
    private final double completedTps;
    private final double successTps;
    private final Double partialFailureRate;
    private final Double failureRate;
    private final String bucketStart;
    private final String bucketEnd;
}
