package com.cmbchina.application.report.api.dto;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/** 规划明细及当前范围内的任务和客户数量。 */
@Getter
@Setter
@Builder
public class PlanDetailRow {
    private String id;
    private String name;
    private String source;
    private String ownerId;
    private String ownerName;
    private String ownerLevel;
    private String periodStart;
    private String periodEnd;
    private java.util.List<ReportOption> skills;
    private long recommendedCustomerCount;
    private long taskCount;
    private long scheduledTaskCount;
    private long executedTaskCount;
    private long pendingTaskCount;
    private long failedTaskCount;
    /** 返回列表快照。 */
    public java.util.List<ReportOption> getSkills() {
        return java.util.Collections.unmodifiableList(com.google.common.collect.Lists.newArrayList(skills));
    }
}
