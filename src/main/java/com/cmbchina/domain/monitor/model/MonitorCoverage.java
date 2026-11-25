package com.cmbchina.domain.monitor.model;

import lombok.Builder;
import lombok.Getter;

/** 离线统计的实际覆盖区间。 */
@Getter
@Builder
public class MonitorCoverage {
    private final String availableFrom;
    private final String availableThrough;
    private final double effectiveSeconds;
    private final String statisticsCompletedAt;
}
