package com.cmbchina.domain.report.support;
import com.cmbchina.domain.report.model.ReportOrganization;
import com.cmbchina.domain.report.model.ReportPlan;
import com.cmbchina.domain.report.model.ReportQuery;
import com.cmbchina.domain.report.model.ReportRelation;
import com.cmbchina.domain.report.model.ReportExecution;
import com.cmbchina.domain.report.model.ReportRecommendation;
import com.cmbchina.domain.report.model.ReportContact;
import java.util.List;
import java.util.Map;
/** 报表分批读取契约。 */
public interface ReportDataSupport {
    /** 技能参与度分子分母，不读取客户结果。 */
    List<com.cmbchina.domain.report.model.ReportAggregate> participation(ReportQuery query);

    /** 返回固定维度的统计汇总，不读取客户明细。 */
    List<com.cmbchina.domain.report.model.ReportAggregate> aggregates(ReportQuery query, com.cmbchina.domain.report.model.ReportDimension dimension,
            boolean planOnly, boolean executedOnly);

    /** 返回客户经理或规划对应的技能展示选项。 */
    List<com.cmbchina.domain.report.model.ReportAggregate> aggregateOptions(ReportQuery query,
            com.cmbchina.domain.report.model.ReportDimension dimension, boolean planOnly, boolean executedOnly);

    /** 聚合顶部统计。 */
    com.cmbchina.domain.report.model.ReportStatistics summarize(ReportQuery query);

    /** 直接读取人员表去重后的机构。 */
    List<ReportOrganization> findFilterOrganizations();

    /** 按规划所属分支行查询技能，未选机构时查询全部技能。 */
    List<com.cmbchina.domain.report.model.ReportSkillOption> findFilterSkills(String bbkId, String orgId);

    /** 读取全部实际非空等级。 */
    List<String> findFilterLevels();

    /** 获取统计规划的来源及展示信息。 */
    List<ReportPlan> findPlans(List<String> ids);

    /** 查找具备任务的人员规划技能关系。 */
    List<ReportRelation> findRelations(ReportQuery query);

    /** 返回技能编号到分类名称的只读映射，未配置分类的技能不在映射中。 */
    Map<String, String> findSkillCategories(List<String> skillIds);

    /** 获取合法分支行的展示名称，不依赖执行结果。 */
    ReportOrganization findOrganization(String bbkId, String orgId);

    /** 获取每天最新执行。 */
    List<ReportExecution> findExecutions(ReportQuery query, List<ReportRelation> relations);

    /** 获取执行对应的成功结果。 */
    List<ReportRecommendation> findRecommendations(List<ReportExecution> executions);

    /** 获取推荐客户在执行当日的按钮和接触行为。 */
    List<com.cmbchina.domain.report.model.ReportCustomerAction> findActions(List<ReportRecommendation> recommendations);

    /** 获取点击关联的接触客户。 */
    List<ReportContact> findContacts(List<ReportRecommendation> recommendations);
}
