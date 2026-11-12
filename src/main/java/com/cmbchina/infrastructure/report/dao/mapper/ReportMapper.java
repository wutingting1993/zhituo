package com.cmbchina.infrastructure.report.dao.mapper;
import com.cmbchina.infrastructure.report.dao.po.ReportSkillCategoryPo;
import com.cmbchina.infrastructure.report.dao.po.ReportOrganizationPo;
import com.cmbchina.infrastructure.report.dao.po.ReportPlanPo;
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
    /** 获取三类来源的规划与负责人信息。 */
    List<ReportPlanPo> plans(@Param("ids") List<String> ids);

    /** 查询日期范围内的规划与技能组合。 */
    List<com.cmbchina.infrastructure.report.dao.po.ReportPlanSkillPo> scheduledSkills(@Param("query") ReportQuery query);

    /** 查询候选规划的人员与有效目标。 */
    List<ReportTargetPo> targets(@Param("query") ReportQuery query, @Param("planIds") List<String> planIds);

    /** 查询目标对应的技能任务。 */
    List<ReportJobPo> jobs(@Param("rows") List<ReportTargetPo> rows, @Param("skills") List<String> skills,
            @Param("scheduled") List<com.cmbchina.infrastructure.report.dao.po.ReportPlanSkillPo> scheduled);

    /** 按技能编号批量查询同入口分类名称。 */
    List<ReportSkillCategoryPo> skillCategories(@Param("ids") List<String> ids);

    /** 查询分支行展示信息。 */
    ReportOrganizationPo organization(@Param("bbkId") String bbkId, @Param("orgId") String orgId);

    /** 校验分支行关系。 */
    long organizationCount(@Param("bbkId") String bbkId, @Param("orgId") String orgId);

    /** 查询排程当天的候选执行。 */
    List<ReportExecutionPo> candidates(@Param("rows") List<ReportRelation> rows, @Param("query") ReportQuery query);

    /** 按选定链路获取执行。 */
    List<ReportExecutionPo> executions(@Param("traces") List<String> traces);

    /** 查询成功的推荐和名单结果。 */
    List<ReportResultPo> results(@Param("rows") List<ReportExecution> rows);

    /** 查询推荐客户的执行当日行为与接触明细。 */
    List<com.cmbchina.infrastructure.report.dao.po.ReportActionPo> actions(@Param("rows") List<ReportRecommendation> rows);

    /** 查询点击关联接触客户。 */
    List<ReportContactPo> contacts(@Param("rows") List<ReportRecommendation> rows);
}
