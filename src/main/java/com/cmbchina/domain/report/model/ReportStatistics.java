package com.cmbchina.domain.report.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/** 报表ReportStatistics数据。 */
@Getter
@Setter
@Builder
public class ReportStatistics {
    private long planCount;
    private long skillCount;
    private long taskCount;
    private long recommendedCustomerCount;
    private long viewedCustomerCount;
    private long contactedCustomerCount;
    private long userCount;
    private long coveredSubBranchCount;
    private MostUsedSkill mostUsedSkill;
}
