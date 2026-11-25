package com.cmbchina.domain.monitor.model;

import lombok.Builder;
import lombok.Getter;

/** 已发布的分组指标行。 */
@Getter
@Builder
public class MonitorMetricRow {
    private final String groupKey;
    private final String metricScope;
    private final String bucketStart;
    private final String bucketEnd;
    private final double effectiveSeconds;
    private final MonitorMetric metric;
}
