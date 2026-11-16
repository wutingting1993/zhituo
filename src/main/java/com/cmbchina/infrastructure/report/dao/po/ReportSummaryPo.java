package com.cmbchina.infrastructure.report.dao.po;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
/** 数据库聚合的顶部统计结果。 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportSummaryPo {
    private long planCount;
    private long skillCount;
    private long taskCount;
    private long scheduledTaskCount;
    private long executedTaskCount;
    private long pendingTaskCount;
    private long failedTaskCount;
    private long recommendedCustomerCount;
    private long viewedCustomerCount;
    private long contactedCustomerCount;
    private long userCount;
    private long coveredSubBranchCount;
    private long mostUsedSkillUserCount;
    private String mostUsedSkillId;
    private String mostUsedSkillName;
}
