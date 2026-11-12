package com.cmbchina.application.report.service;
import com.cmbchina.application.report.api.PlanCoverageApplicationService;
import com.cmbchina.application.report.api.dto.PlanCoverageResponse;
import com.cmbchina.application.report.api.dto.PlanCoverageRow;
import com.cmbchina.application.report.api.dto.PlanDetailsRequest;
import com.cmbchina.application.report.api.dto.PlanDetailsResponse;
import com.cmbchina.application.report.api.dto.PlanDetailRow;
import com.cmbchina.application.report.api.dto.ReportOption;
import com.cmbchina.application.report.api.dto.ReportSummaryRequest;
import com.cmbchina.domain.report.model.ReportExecution;
import com.cmbchina.domain.report.model.ReportPlan;
import com.cmbchina.domain.report.model.ReportRecommendation;
import com.cmbchina.domain.report.model.ReportQuery;
import com.cmbchina.domain.report.model.ReportRelation;
import com.cmbchina.domain.report.model.ReportStatistics;
import com.cmbchina.domain.report.service.ReportStatisticsService;
import com.cmbchina.domain.report.support.ReportDataSupport;
import com.cmbchina.infrastructure.common.ResponseEnum;
import com.cmbchina.infrastructure.common.exception.BusinessException;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Comparator;
import com.cmbchina.application.report.api.dto.PlanSourceRow;
import com.cmbchina.application.report.api.dto.PlanSourcesResponse;
import com.cmbchina.application.report.api.dto.PlanSourcesRequest;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
/** 复用存量执行范围，按机构编排覆盖与规划明细。 */
@Service
public class PlanCoverageApplicationServiceImpl implements PlanCoverageApplicationService {
    @Autowired
    private ReportDataSupport data;
    @Autowired
    private ReportStatisticsService statistics;

    @Override
    public PlanCoverageResponse coverage(ReportSummaryRequest request) {
        List<ReportExecution> executions = load(request).stream().filter(e -> e.getActualTime() != null).collect(Collectors.toList());
        Map<String, ReportPlan> plans = plans(executions);
        List<ReportExecution> included = included(executions, plans);
        Map<String, List<ReportExecution>> branches = included.stream()
                .collect(Collectors.groupingBy(e -> e.getRelation().getBbkId(), java.util.TreeMap::new, Collectors.toList()));
        List<PlanCoverageRow> rows = Lists.newArrayList();
        for (List<ReportExecution> branch : branches.values()) {
            ReportRelation first = branch.get(0).getRelation();
            Map<String, List<ReportExecution>> organizations = branch.stream()
                    .collect(Collectors.groupingBy(e -> e.getRelation().getOrgId(), java.util.TreeMap::new, Collectors.toList()));
            List<PlanCoverageRow> children = Lists.newArrayList();
            for (List<ReportExecution> organization : organizations.values()) {
                ReportRelation org = organization.get(0).getRelation();
                children.add(row(org.getOrgId(), org.getOrgName(), organization, plans, Lists.newArrayList()));
            }
            rows.add(row(first.getBbkId(), first.getBbkName(), branch, plans, children));
        }
        return PlanCoverageResponse.builder().branches(rows).build();
    }

    @Override
    public PlanSourcesResponse sources(PlanSourcesRequest request) {
        List<ReportExecution> executions = load(request).stream().filter(e -> e.getActualTime() != null).collect(Collectors.toList());
        Map<String, ReportPlan> plans = plans(executions);
        Map<String, List<ReportExecution>> branches = included(executions, plans).stream()
                .collect(Collectors.groupingBy(e -> e.getRelation().getBbkId()));
        List<PlanSourceRow> rows = Lists.newArrayList();
        for (List<ReportExecution> branch : branches.values()) {
            ReportRelation first = branch.get(0).getRelation();
            rows.add(PlanSourceRow.builder().id(first.getBbkId()).name(first.getBbkName())
                    .planCount(branch.stream().map(e -> e.getRelation().getPlanId()).distinct().count()).myCount(sourceCount(branch, plans, "我的关注"))
                    .branchCount(sourceCount(branch, plans, "分行关注")).presidentCount(sourceCount(branch, plans, "行长关注")).build());
        }
        return PlanSourcesResponse.builder()
                .branches(rows.stream().sorted(Comparator.comparingLong(PlanSourceRow::getPlanCount).reversed().thenComparing(PlanSourceRow::getId))
                        .limit(request.getTopN()).collect(Collectors.toList()))
                .build();
    }

    private List<ReportExecution> load(ReportSummaryRequest request) {
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw invalid("开始日期不能晚于结束日期");
        }
        ReportQuery query = ReportQuery.builder().bbkId(request.getBbkId()).orgId(request.getOrgId()).level(request.getLevel())
                .skillIds(request.getSkillIds()).startDate(request.getStartDate()).endDate(request.getEndDate()).build();
        return data.findExecutions(query, data.findRelations(query));
    }

    private BusinessException invalid(String message) {
        return new BusinessException(ResponseEnum.VALIDATION_ERROR.getCode(), message, null);
    }

    private Map<String, ReportPlan> plans(List<ReportExecution> executions) {
        List<String> ids = executions.stream().map(e -> e.getRelation().getPlanId()).distinct().collect(Collectors.toList());
        return data.findPlans(ids).stream().collect(Collectors.toMap(ReportPlan::getId, Function.identity(), (first, second) -> first));
    }

    private List<ReportExecution> included(List<ReportExecution> executions, Map<String, ReportPlan> plans) {
        return executions.stream().filter(e -> plans.containsKey(e.getRelation().getPlanId())).collect(Collectors.toList());
    }

    private long sourceCount(List<ReportExecution> executions, Map<String, ReportPlan> plans, String source) {
        return executions.stream().map(e -> e.getRelation().getPlanId()).distinct().filter(id -> source.equals(plans.get(id).getSource())).count();
    }

    private PlanCoverageRow row(String id, String name, List<ReportExecution> executions, Map<String, ReportPlan> plans,
            List<PlanCoverageRow> children) {
        return PlanCoverageRow.builder().id(id).name(name).planCount(executions.stream().map(e -> e.getRelation().getPlanId()).distinct().count())
                .myCount(sourceCount(executions, plans, "我的关注")).branchCount(sourceCount(executions, plans, "分行关注"))
                .presidentCount(sourceCount(executions, plans, "行长关注"))
                .subBranchCount(
                        executions.stream().map(e -> Lists.newArrayList(e.getRelation().getBbkId(), e.getRelation().getOrgId())).distinct().count())
                .children(children).build();
    }

    @Override
    public PlanDetailsResponse details(PlanDetailsRequest request) {
        validateScope(request);
        List<ReportExecution> executions = load(request.getFilters()).stream().filter(e -> request.getScopeBbkId().equals(e.getRelation().getBbkId()))
                .filter(e -> request.getScopeOrgId() == null || request.getScopeOrgId().equals(e.getRelation().getOrgId()))
                .collect(Collectors.toList());
        Map<String, ReportPlan> plans = plans(executions);
        List<ReportExecution> scoped = included(executions, plans);
        List<ReportRecommendation> recommendations = data.findRecommendations(scoped);
        ReportStatistics summary = statistics.summarize(scoped, recommendations, Lists.newArrayList());
        String name = executions.isEmpty()
                ? null
                : request.getScopeOrgId() == null ? executions.get(0).getRelation().getBbkName() : executions.get(0).getRelation().getOrgName();
        return PlanDetailsResponse.builder().bbkId(request.getScopeBbkId()).orgId(request.getScopeOrgId()).name(name)
                .planCount(summary.getPlanCount()).recommendedCustomerCount(summary.getRecommendedCustomerCount()).taskCount(summary.getTaskCount())
                .scheduledTaskCount(summary.getScheduledTaskCount()).executedTaskCount(summary.getExecutedTaskCount())
                .pendingTaskCount(summary.getPendingTaskCount()).failedTaskCount(summary.getFailedTaskCount()).skillCount(summary.getSkillCount())
                .myCount(sourceCount(scoped, plans, "我的关注")).branchCount(sourceCount(scoped, plans, "分行关注"))
                .presidentCount(sourceCount(scoped, plans, "行长关注")).plans(detailRows(scoped, plans, request.getSource(), recommendations)).build();
    }

    private void validateScope(PlanDetailsRequest request) {
        ReportSummaryRequest filters = request.getFilters();
        if (request.getScopeBbkId() == null || request.getScopeBbkId().trim().isEmpty()) {
            throw invalid("请选择下钻分行");
        }
        if (filters.getBbkId() != null && !filters.getBbkId().equals(request.getScopeBbkId())
                || filters.getOrgId() != null && request.getScopeOrgId() != null && !filters.getOrgId().equals(request.getScopeOrgId())) {
            throw invalid("下钻机构超出当前检索范围");
        }
        validateScopeOptions(request);
    }

    private void validateScopeOptions(PlanDetailsRequest request) {
        if (request.getSource() != null && !Lists.newArrayList("全部", "我的关注", "分行关注", "行长关注").contains(request.getSource())) {
            throw invalid("规划来源须为全部、我的关注、分行关注或行长关注");
        }
    }

    private List<PlanDetailRow> detailRows(List<ReportExecution> executions, Map<String, ReportPlan> plans, String source,
            List<ReportRecommendation> recommendations) {
        Map<String, List<ReportExecution>> grouped = executions.stream()
                .collect(Collectors.groupingBy(e -> e.getRelation().getPlanId(), java.util.TreeMap::new, Collectors.toList()));
        List<PlanDetailRow> result = Lists.newArrayList();
        for (Map.Entry<String, List<ReportExecution>> entry : grouped.entrySet()) {
            ReportPlan plan = plans.get(entry.getKey());
            if (source == null || "全部".equals(source) || source.equals(plan.getSource())) {
                List<ReportRecommendation> matches = recommendations.stream()
                        .filter(r -> plan.getId().equals(r.getExecution().getRelation().getPlanId())).collect(Collectors.toList());
                result.add(detail(plan, entry.getValue(), matches));
            }
        }
        return result;
    }

    private PlanDetailRow detail(ReportPlan plan, List<ReportExecution> executions, List<ReportRecommendation> recommendations) {
        ReportStatistics summary = statistics.summarize(executions, recommendations, Lists.newArrayList());
        Map<String, ReportOption> skills = executions.stream().map(ReportExecution::getRelation).collect(Collectors.toMap(ReportRelation::getSkillId,
                r -> ReportOption.builder().id(r.getSkillId()).name(r.getSkillName()).build(), (first, second) -> first, java.util.TreeMap::new));
        return PlanDetailRow.builder().id(plan.getId()).name(plan.getName()).source(plan.getSource()).ownerId(plan.getOwnerId())
                .ownerName(plan.getOwnerName()).ownerLevel(plan.getOwnerLevel()).periodStart(plan.getPeriodStart()).periodEnd(plan.getPeriodEnd())
                .skills(Lists.newArrayList(skills.values())).recommendedCustomerCount(summary.getRecommendedCustomerCount())
                .taskCount(summary.getTaskCount()).scheduledTaskCount(summary.getScheduledTaskCount())
                .executedTaskCount(summary.getExecutedTaskCount()).pendingTaskCount(summary.getPendingTaskCount())
                .failedTaskCount(summary.getFailedTaskCount()).build();
    }
}
