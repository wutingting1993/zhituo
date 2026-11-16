package com.cmbchina.application.report.service;
import com.cmbchina.application.report.api.PlanCoverageApplicationService;
import com.cmbchina.application.report.api.dto.PlanCoverageResponse;
import com.cmbchina.application.report.api.dto.PlanCoverageRow;
import com.cmbchina.application.report.api.dto.PlanDetailsRequest;
import com.cmbchina.application.report.api.dto.PlanDetailsResponse;
import com.cmbchina.application.report.api.dto.PlanDetailRow;
import com.cmbchina.application.report.api.dto.ReportOption;
import com.cmbchina.application.report.api.dto.ReportSummaryRequest;
import com.cmbchina.domain.report.model.ReportAggregate;
import com.cmbchina.domain.report.model.ReportDimension;
import com.cmbchina.domain.report.model.ReportPlan;
import com.cmbchina.domain.report.model.ReportQuery;
import com.cmbchina.domain.report.model.ReportStatistics;
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

    @Override
    public PlanCoverageResponse coverage(ReportSummaryRequest request) {
        ReportQuery query = query(request);
        Map<String, List<ReportAggregate>> children = data.aggregates(query, ReportDimension.BRANCH, true, true).stream()
                .collect(Collectors.groupingBy(ReportAggregate::getKey));
        List<PlanCoverageRow> rows = Lists.newArrayList();
        for (ReportAggregate branch : data.aggregates(query, ReportDimension.BANK, true, true)) {
            List<PlanCoverageRow> organizations = children.getOrDefault(branch.getKey(), Lists.newArrayList()).stream()
                    .map(child -> row(child.getSecondKey(), child.getRelation().getOrgName(), child, Lists.newArrayList()))
                    .collect(Collectors.toList());
            rows.add(row(branch.getKey(), branch.getRelation().getBbkName(), branch, organizations));
        }
        return PlanCoverageResponse.builder().branches(rows).build();
    }

    @Override
    public PlanSourcesResponse sources(PlanSourcesRequest request) {
        List<PlanSourceRow> rows = data.aggregates(query(request), ReportDimension.BANK, true, true).stream()
                .map(group -> PlanSourceRow.builder().id(group.getKey()).name(group.getRelation().getBbkName())
                        .planCount(group.getMetrics().getStatistics().getPlanCount()).myCount(group.getMyCount()).branchCount(group.getBranchCount())
                        .presidentCount(group.getPresidentCount()).build())
                .collect(Collectors.toList());
        return PlanSourcesResponse.builder()
                .branches(rows.stream().sorted(Comparator.comparingLong(PlanSourceRow::getPlanCount).reversed().thenComparing(PlanSourceRow::getId))
                        .limit(request.getTopN()).collect(Collectors.toList()))
                .build();
    }

    private ReportQuery query(ReportSummaryRequest request) {
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw invalid("开始日期不能晚于结束日期");
        }
        return ReportQuery.builder().bbkId(request.getBbkId()).orgId(request.getOrgId()).level(request.getLevel()).skillIds(request.getSkillIds())
                .startDate(request.getStartDate()).endDate(request.getEndDate()).build();
    }

    private BusinessException invalid(String message) {
        return new BusinessException(ResponseEnum.VALIDATION_ERROR.getCode(), message, null);
    }

    private PlanCoverageRow row(String id, String name, ReportAggregate group, List<PlanCoverageRow> children) {
        return PlanCoverageRow.builder().id(id).name(name).planCount(group.getMetrics().getStatistics().getPlanCount()).myCount(group.getMyCount())
                .branchCount(group.getBranchCount()).presidentCount(group.getPresidentCount())
                .subBranchCount(group.getMetrics().getStatistics().getCoveredSubBranchCount()).children(children).build();
    }

    @Override
    public PlanDetailsResponse details(PlanDetailsRequest request) {
        validateScope(request);
        ReportSummaryRequest filters = request.getFilters();
        ReportQuery scoped = query(ReportSummaryRequest.builder().startDate(filters.getStartDate()).endDate(filters.getEndDate())
                .bbkId(request.getScopeBbkId()).orgId(request.getScopeOrgId() == null ? filters.getOrgId() : request.getScopeOrgId())
                .level(filters.getLevel()).skillIds(filters.getSkillIds()).build());
        List<ReportAggregate> totals = data.aggregates(scoped, ReportDimension.TOTAL, true, false);
        ReportAggregate total = totals.isEmpty()
                ? ReportAggregate.builder()
                        .metrics(com.cmbchina.domain.report.model.ExecutionMetrics.builder().statistics(ReportStatistics.builder().build()).build())
                        .build()
                : totals.get(0);
        List<ReportAggregate> names = data.aggregateOptions(scoped, ReportDimension.BANK, false, false);
        String name = names.isEmpty()
                ? null
                : request.getScopeOrgId() == null ? names.get(0).getRelation().getBbkName() : names.get(0).getRelation().getOrgName();
        ReportStatistics summary = total.getMetrics().getStatistics();
        return PlanDetailsResponse.builder().bbkId(request.getScopeBbkId()).orgId(request.getScopeOrgId()).name(name)
                .planCount(summary.getPlanCount()).recommendedCustomerCount(summary.getRecommendedCustomerCount()).taskCount(summary.getTaskCount())
                .scheduledTaskCount(summary.getScheduledTaskCount()).executedTaskCount(summary.getExecutedTaskCount())
                .pendingTaskCount(summary.getPendingTaskCount()).failedTaskCount(summary.getFailedTaskCount()).skillCount(summary.getSkillCount())
                .myCount(total.getMyCount()).branchCount(total.getBranchCount()).presidentCount(total.getPresidentCount())
                .plans(detailRows(scoped, request.getSource())).build();
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

    private List<PlanDetailRow> detailRows(ReportQuery query, String source) {
        List<ReportAggregate> groups = data.aggregates(query, ReportDimension.PLAN, true, false);
        Map<String, ReportPlan> plans = data.findPlans(groups.stream().map(ReportAggregate::getKey).collect(Collectors.toList())).stream()
                .collect(Collectors.toMap(ReportPlan::getId, Function.identity(), (first, second) -> first));
        Map<String, List<ReportOption>> skills = data.aggregateOptions(query, ReportDimension.PLAN_SKILL, true, false).stream()
                .collect(Collectors.groupingBy(ReportAggregate::getKey,
                        Collectors.mapping(
                                row -> ReportOption.builder().id(row.getRelation().getSkillId()).name(row.getRelation().getSkillName()).build(),
                                Collectors.toList())));
        List<PlanDetailRow> result = Lists.newArrayList();
        for (ReportAggregate group : groups) {
            ReportPlan plan = plans.get(group.getKey());
            if (source == null || "全部".equals(source) || source.equals(plan.getSource())) {
                result.add(detail(plan, group.getMetrics().getStatistics(), skills.getOrDefault(group.getKey(), Lists.newArrayList())));
            }
        }
        return result;
    }

    private PlanDetailRow detail(ReportPlan plan, ReportStatistics summary, List<ReportOption> skills) {
        return PlanDetailRow.builder().id(plan.getId()).name(plan.getName()).source(plan.getSource()).ownerId(plan.getOwnerId())
                .ownerName(plan.getOwnerName()).ownerLevel(plan.getOwnerLevel()).periodStart(plan.getPeriodStart()).periodEnd(plan.getPeriodEnd())
                .skills(skills).recommendedCustomerCount(summary.getRecommendedCustomerCount()).taskCount(summary.getTaskCount())
                .scheduledTaskCount(summary.getScheduledTaskCount()).executedTaskCount(summary.getExecutedTaskCount())
                .pendingTaskCount(summary.getPendingTaskCount()).failedTaskCount(summary.getFailedTaskCount()).build();
    }
}
