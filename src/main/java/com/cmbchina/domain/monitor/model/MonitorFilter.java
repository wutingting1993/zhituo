package com.cmbchina.domain.monitor.model;

import lombok.Builder;
import lombok.Getter;

/** 离线统计的分组和业务筛选条件。 */
@Getter
@Builder
public class MonitorFilter {
    private final MonitorRange range;
    private final String groupBy;
    private final String interfaceId;
    private final String agentId;
    private final boolean unattributedOnly;
}
