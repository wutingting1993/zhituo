package com.cmbchina.infrastructure.report.dao.converter;
import com.cmbchina.domain.report.model.ReportPlan;
import com.cmbchina.infrastructure.report.dao.po.ReportPlanPo;
import com.cmbchina.domain.report.model.ReportRelation;
import com.cmbchina.domain.report.model.ReportExecution;
import com.cmbchina.infrastructure.report.dao.po.ReportTargetPo;
import com.cmbchina.infrastructure.report.dao.po.ReportJobPo;
import com.cmbchina.infrastructure.report.dao.po.ReportExecutionPo;
/** 持久化记录到领域数据的边界转换。 */
public final class ReportDataConverter {
    private ReportDataConverter() {
    }

    /** 转换分组统计结果，不携带客户明细。 */
    public static com.cmbchina.domain.report.model.ReportAggregate aggregate(com.cmbchina.infrastructure.report.dao.po.ReportAggregatePo row) {
        com.cmbchina.domain.report.model.ReportStatistics statistics = com.cmbchina.domain.report.model.ReportStatistics.builder()
                .planCount(row.getPlanCount()).skillCount(row.getSkillCount()).taskCount(row.getTaskCount())
                .scheduledTaskCount(row.getScheduledTaskCount()).executedTaskCount(row.getExecutedTaskCount())
                .pendingTaskCount(row.getPendingTaskCount()).failedTaskCount(row.getFailedTaskCount())
                .recommendedCustomerCount(row.getRecommendedCustomerCount()).viewedCustomerCount(row.getViewedCustomerCount())
                .contactedCustomerCount(row.getContactedCustomerCount()).userCount(row.getUserCount())
                .coveredSubBranchCount(row.getCoveredSubBranchCount()).build();
        ReportRelation relation = ReportRelation.builder().bbkId(row.getBbkId()).bbkName(row.getBbkName()).orgId(row.getOrgId())
                .orgName(row.getOrgName()).sapId(row.getSapId()).userName(row.getUserName()).level(row.getLevel()).planId(row.getPlanId())
                .skillId(row.getSkillId()).skillName(row.getSkillName()).build();
        return com.cmbchina.domain.report.model.ReportAggregate.builder().key(row.getKey()).secondKey(row.getSecondKey()).day(row.getDay())
                .relation(relation)
                .metrics(com.cmbchina.domain.report.model.ExecutionMetrics.builder().statistics(statistics)
                        .insightCustomerCount(row.getInsightCustomerCount()).callCustomerCount(row.getCallCustomerCount()).build())
                .myCount(row.getMyCount()).branchCount(row.getBranchCount()).presidentCount(row.getPresidentCount())
                .assignedManagerCount(row.getAssignedManagerCount()).participantCount(row.getParticipantCount()).build();
    }

    /** 转换数据库聚合结果。 */
    public static com.cmbchina.domain.report.model.ReportStatistics summary(com.cmbchina.infrastructure.report.dao.po.ReportSummaryPo row) {
        com.cmbchina.domain.report.model.MostUsedSkill skill = row.getMostUsedSkillId() == null
                ? null
                : com.cmbchina.domain.report.model.MostUsedSkill.builder().id(row.getMostUsedSkillId()).name(row.getMostUsedSkillName())
                        .userCount(row.getMostUsedSkillUserCount()).build();
        return com.cmbchina.domain.report.model.ReportStatistics.builder().planCount(row.getPlanCount()).skillCount(row.getSkillCount())
                .taskCount(row.getTaskCount()).scheduledTaskCount(row.getScheduledTaskCount()).executedTaskCount(row.getExecutedTaskCount())
                .pendingTaskCount(row.getPendingTaskCount()).failedTaskCount(row.getFailedTaskCount())
                .recommendedCustomerCount(row.getRecommendedCustomerCount()).viewedCustomerCount(row.getViewedCustomerCount())
                .contactedCustomerCount(row.getContactedCustomerCount()).userCount(row.getUserCount())
                .coveredSubBranchCount(row.getCoveredSubBranchCount()).mostUsedSkill(skill).build();
    }

    /** 转换规划来源及负责人信息。 */
    public static ReportPlan plan(ReportPlanPo row) {
        return ReportPlan.builder().id(row.getId()).name(row.getName()).source(row.getSource()).ownerId(row.getOwnerId())
                .ownerName(row.getOwnerName()).ownerLevel(row.getOwnerLevel()).periodStart(row.getPeriodStart()).periodEnd(row.getPeriodEnd())
                .build();
    }

    /** 组合人员目标和技能任务。 */
    public static ReportRelation relation(ReportTargetPo target, ReportJobPo job) {
        return ReportRelation.builder().bbkId(target.getBbkId()).bbkName(target.getBbkName()).orgId(target.getOrgId()).orgName(target.getOrgName())
                .level(target.getLevel()).sapId(target.getSapId()).userName(target.getUserName()).planId(target.getPlanId())
                .createdAt(target.getCreatedAt()).skillId(job.getSkillId()).skillName(job.getSkillName()).jobId(job.getJobId()).build();
    }

    /** 保留人员机构关系和执行信息。 */
    public static ReportExecution execution(ReportRelation relation, ReportExecutionPo row) {
        return ReportExecution.builder().relation(relation).traceId(row.getTraceId()).actualTime(row.getActualTime())
                .executionDay(row.getActualTime().toLocalDate()).read(row.isRead()).build();
    }
}
