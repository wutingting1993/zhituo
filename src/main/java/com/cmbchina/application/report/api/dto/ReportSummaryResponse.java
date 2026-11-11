package com.cmbchina.application.report.api.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
/** 报表ReportSummaryResponse数据。 */
@Getter
@Setter
@Builder
public class ReportSummaryResponse {
    private long planCount;
    private long skillCount;
    private long taskCount;
    private long recommendedCustomerCount;
    private long coveredSubBranchCount;
    private MostUsedSkillResponse mostUsedSkill;
    private Long averageTaskCount;
    private Long averageRecommendedCustomerCount;
    private BigDecimal viewRate;
    private BigDecimal contactRate;
    private BigDecimal benchmarkViewRate;
    private BigDecimal benchmarkContactRate;
    private BigDecimal viewRateDifference;
    private BigDecimal contactRateDifference;
}
