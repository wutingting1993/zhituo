package com.cmbchina.infrastructure.monitor.dao.po;

import lombok.Getter;
import lombok.Setter;

/** 离线统计指标行。 */
@Getter
@Setter
public class MonitorStatisticsRow {
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
    private Long activeSecondP95Qps;
    private Long activeSecondP95SuccessTps;
    private Long peakQps;
    private Double errorRate;
    private Double timeoutRate;
    private Double cacheHitRate;
    private Double p95Millis;
    private Double p99Millis;
}
