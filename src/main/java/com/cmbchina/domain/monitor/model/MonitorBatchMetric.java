package com.cmbchina.domain.monitor.model;

import lombok.Getter;
import lombok.Setter;

/** 保存已覆盖分钟或整个区间的批量请求指标。 */
@Getter
@Setter
public class MonitorBatchMetric {
    private long startedCount;
    private long successCount;
    private long partialFailureCount;
    private long failureCount;
    private long cancelledCount;
    private long completedCount;
    private long latencySampleCount;
    private Double averageMillis;
    private Double p95Millis;
    private double qps;
    private double completedTps;
    private double successTps;
    private Double partialFailureRate;
    private Double failureRate;
    private String bucketStart;
    private String bucketEnd;
    private long effectiveSeconds;
    private String statisticsCompletedAt;
}
