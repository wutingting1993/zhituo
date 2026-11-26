package com.cmbchina.infrastructure.monitor.dao.po;

import lombok.Getter;
import lombok.Setter;

/** 离线统计覆盖行。 */
@Getter
@Setter
public class MonitorCoverageRow {
    private String availableFrom;
    private String availableThrough;
    private double effectiveSeconds;
    private String statisticsCompletedAt;
}
