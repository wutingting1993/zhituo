package com.cmbchina.infrastructure.report.dao.mapper;
import com.cmbchina.domain.report.model.ReportQuery;
import com.cmbchina.domain.report.model.ReportRelation;
import com.cmbchina.domain.report.model.ReportRecommendation;
import com.cmbchina.domain.report.model.ReportExecution;
import com.cmbchina.infrastructure.report.dao.po.ReportTargetPo;
import com.cmbchina.infrastructure.report.dao.po.ReportJobPo;
import com.cmbchina.infrastructure.report.dao.po.ReportExecutionPo;
import com.cmbchina.infrastructure.report.dao.po.ReportResultPo;
import com.cmbchina.infrastructure.report.dao.po.ReportContactPo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
/** 报表只读分阶段查询。 */
@Mapper
public interface ReportMapper {
    /** 查询人员与有效规划目标。 */
    List<ReportTargetPo> targets(@Param("query") ReportQuery query);

    /** 查询目标对应的技能任务。 */
    List<ReportJobPo> jobs(@Param("rows") List<ReportTargetPo> rows);

    /** 校验分支行关系。 */
    long organizationCount(@Param("bbkId") String bbkId, @Param("orgId") String orgId);

    /** 查询排程当天的候选执行。 */
    List<ReportExecutionPo> candidates(@Param("rows") List<ReportRelation> rows, @Param("query") ReportQuery query);

    /** 按选定链路获取执行。 */
    List<ReportExecutionPo> executions(@Param("traces") List<String> traces);

    /** 查询成功的推荐和名单结果。 */
    List<ReportResultPo> results(@Param("rows") List<ReportExecution> rows);

    /** 查询点击关联接触客户。 */
    List<ReportContactPo> contacts(@Param("rows") List<ReportRecommendation> rows);
}
