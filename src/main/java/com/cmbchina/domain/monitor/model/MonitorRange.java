package com.cmbchina.domain.monitor.model;

import lombok.Builder;
import lombok.Getter;

/** 离线统计所用的时间范围与桶粒度。 */
@Getter
@Builder
public class MonitorRange {
    private final String startTime;
    private final String endTime;
    private final int bucketSeconds;
}
