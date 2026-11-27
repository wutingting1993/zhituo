package com.cmbchina.domain.monitor.model;

import lombok.Builder;
import lombok.Getter;

/** 同一统计口径的指标值。 */
@Getter
@Builder
public class MonitorMetric {
    private final long startedCount;
    private final long successCount;
    private final long failureCount;
    private final long timeoutCount;
    private final long cancelledCount;
    private final long cacheReadCount;
    private final long cacheHitCount;
    private final long expertCount;
    private final long interfaceCount;
    private final long latencySampleCount;
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
