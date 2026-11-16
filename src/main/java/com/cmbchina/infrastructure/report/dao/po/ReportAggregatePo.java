package com.cmbchina.infrastructure.report.dao.po;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;
/** 数据库分组聚合及展示结果。 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportAggregatePo {
    private String key;
    private String secondKey;
    private String bbkId;
    private String bbkName;
    private String orgId;
    private String orgName;
    private String sapId;
    private String userName;
    private String level;
    private String planId;
    private String skillId;
    private String skillName;
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
    private long insightCustomerCount;
    private long callCustomerCount;
    private long myCount;
    private long branchCount;
    private long presidentCount;
    private long assignedManagerCount;
    private long participantCount;
    private LocalDate day;
}
