package com.cmbchina.domain.monitor.model;

import lombok.Builder;
import lombok.Getter;

/** 离线统计重算参数。 */
@Getter
@Builder
public class MonitorRefreshCommand {
    private final String periodType;
    private final long offset;
}
