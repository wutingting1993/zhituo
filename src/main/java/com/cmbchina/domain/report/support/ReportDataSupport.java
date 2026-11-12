package com.cmbchina.domain.report.support;
import com.cmbchina.domain.report.model.ReportOrganization;
import com.cmbchina.domain.report.model.ReportPlan;
import com.cmbchina.domain.report.model.ReportQuery;
import com.cmbchina.domain.report.model.ReportRelation;
import com.cmbchina.domain.report.model.ReportExecution;
import com.cmbchina.domain.report.model.ReportRecommendation;
import com.cmbchina.domain.report.model.ReportContact;
import java.util.List;
/** 报表分批读取契约。 */
public interface ReportDataSupport {
    /** 获取统计规划的来源及展示信息。 */
    List<ReportPlan> findPlans(List<String> ids);

    /** 查找具备任务的人员规划技能关系。 */
    List<ReportRelation> findRelations(ReportQuery query);

    /** 获取合法分支行的展示名称，不依赖执行结果。 */
    ReportOrganization findOrganization(String bbkId, String orgId);

    /** 按人员机构信息校验分支行归属。 */
    boolean matchesOrganization(String bbkId, String orgId);

    /** 获取每天最新执行。 */
    List<ReportExecution> findExecutions(ReportQuery query, List<ReportRelation> relations);

    /** 获取执行对应的成功结果。 */
    List<ReportRecommendation> findRecommendations(List<ReportExecution> executions);

    /** 获取推荐客户在执行当日的按钮和接触行为。 */
    List<com.cmbchina.domain.report.model.ReportCustomerAction> findActions(List<ReportRecommendation> recommendations);

    /** 获取点击关联的接触客户。 */
    List<ReportContact> findContacts(List<ReportRecommendation> recommendations);
}
