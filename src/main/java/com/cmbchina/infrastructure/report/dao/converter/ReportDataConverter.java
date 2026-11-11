package com.cmbchina.infrastructure.report.dao.converter;
import com.cmbchina.domain.report.model.ReportRelation;
import com.cmbchina.domain.report.model.ReportExecution;
import com.cmbchina.infrastructure.report.dao.po.ReportTargetPo;
import com.cmbchina.infrastructure.report.dao.po.ReportJobPo;
import com.cmbchina.infrastructure.report.dao.po.ReportExecutionPo;
/** 持久化记录到领域数据的边界转换。 */
public final class ReportDataConverter {
    private ReportDataConverter() {
    }

    /** 组合人员目标和技能任务。 */
    public static ReportRelation relation(ReportTargetPo target, ReportJobPo job) {
        return ReportRelation.builder().bbkId(target.getBbkId()).bbkName(target.getBbkName()).orgId(target.getOrgId()).orgName(target.getOrgName())
                .level(target.getLevel()).sapId(target.getSapId()).planId(target.getPlanId()).createdAt(target.getCreatedAt())
                .skillId(job.getSkillId()).skillName(job.getSkillName()).jobId(job.getJobId()).build();
    }

    /** 保留人员机构关系和执行信息。 */
    public static ReportExecution execution(ReportRelation relation, ReportExecutionPo row) {
        return ReportExecution.builder().relation(relation).traceId(row.getTraceId()).actualTime(row.getActualTime())
                .executionDay(row.getActualTime().toLocalDate()).read(row.isRead()).build();
    }
}
