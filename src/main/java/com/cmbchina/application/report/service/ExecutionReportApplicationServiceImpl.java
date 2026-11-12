package com.cmbchina.application.report.service;
import com.cmbchina.application.report.api.ExecutionReportApplicationService;
import com.cmbchina.application.report.api.dto.ExecutionTrendPoint;
import com.cmbchina.application.report.api.dto.ExecutionTrendResponse;
import com.cmbchina.application.report.api.dto.SkillParticipationRequest;
import com.cmbchina.application.report.api.dto.SkillParticipationRow;
import com.cmbchina.application.report.api.dto.SkillParticipationResponse;
import com.cmbchina.application.report.api.dto.AttentionBranchRow;
import com.cmbchina.application.report.api.dto.AttentionBranchesResponse;
import com.cmbchina.application.report.api.dto.ExecutionBenchmarkResponse;
import com.cmbchina.application.report.api.dto.ExecutionBranchRow;
import com.cmbchina.application.report.api.dto.ExecutionBranchesRequest;
import com.cmbchina.application.report.api.dto.ExecutionBranchesResponse;
import com.cmbchina.application.report.api.dto.ExecutionDetailsRequest;
import com.cmbchina.application.report.api.dto.ExecutionLossResponse;
import com.cmbchina.application.report.api.dto.ExecutionManagerDetailsResponse;
import com.cmbchina.application.report.api.dto.ExecutionManagerRow;
import com.cmbchina.application.report.api.dto.ExecutionManagerSummary;
import com.cmbchina.application.report.api.dto.ExecutionMetricsResponse;
import com.cmbchina.application.report.api.dto.ExecutionOverviewResponse;
import com.cmbchina.application.report.api.dto.ExecutionSkillDetailsResponse;
import com.cmbchina.application.report.api.dto.ExecutionSkillOption;
import com.cmbchina.application.report.api.dto.ExecutionSkillRow;
import com.cmbchina.application.report.api.dto.ExecutionSkillSummary;
import com.cmbchina.application.report.api.dto.ReportSummaryRequest;
import com.cmbchina.domain.report.model.ExecutionDataset;
import com.cmbchina.domain.report.model.ExecutionMetrics;
import com.cmbchina.domain.report.model.ReportExecution;
import com.cmbchina.domain.report.model.ReportCustomerAction;
import com.cmbchina.domain.report.model.ReportOrganization;
import com.cmbchina.domain.report.model.ReportQuery;
import com.cmbchina.domain.report.model.ReportRecommendation;
import com.cmbchina.domain.report.model.ReportRelation;
import com.cmbchina.domain.report.model.ReportStatistics;
import com.cmbchina.domain.report.service.ExecutionStatisticsService;
import com.cmbchina.domain.report.support.ExecutionCalculations;
import com.cmbchina.domain.report.support.ReportDataSupport;
import com.cmbchina.infrastructure.common.ResponseEnum;
import com.cmbchina.infrastructure.common.exception.BusinessException;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
/** 复用存量执行链路，按机构、人员和技能独立去重。 */
@Service
public class ExecutionReportApplicationServiceImpl implements ExecutionReportApplicationService {
    private static final int DEFAULT_PARTICIPATION_TOP_N = 5;
    private static final int ATTENTION_LIMIT = 4;
    private static final List<String> SORT_KEYS = java.util.Collections.unmodifiableList(
            Lists.newArrayList("skillCount", "taskCount", "recommendedCustomerCount", "viewRate", "insightRate", "callRate", "contactRate"));
    @Autowired
    private ReportDataSupport data;
    @Autowired
    private ExecutionStatisticsService statistics;

    @Override
    public ExecutionTrendResponse trend(ReportSummaryRequest request) {
        ExecutionDataset dataset = load(request);
        Map<LocalDate, List<ReportExecution>> executions = dataset.getExecutions().stream()
                .collect(Collectors.groupingBy(ReportExecution::getExecutionDay));
        Map<LocalDate, List<ReportRecommendation>> recommendations = dataset.getRecommendations().stream()
                .collect(Collectors.groupingBy(r -> r.getExecution().getExecutionDay()));
        Map<LocalDate, List<ReportCustomerAction>> actions = dataset.getActions().stream()
                .collect(Collectors.groupingBy(a -> a.getRecommendation().getExecution().getExecutionDay()));
        List<ExecutionTrendPoint> points = Lists.newArrayList();
        LocalDate day = request.getStartDate();
        while (true) {
            ExecutionMetrics metrics = statistics.summarize(executions.getOrDefault(day, java.util.Collections.emptyList()),
                    recommendations.getOrDefault(day, java.util.Collections.emptyList()),
                    actions.getOrDefault(day, java.util.Collections.emptyList()));
            points.add(ExecutionTrendPoint.builder().date(day).metrics(response(metrics)).build());
            if (day.equals(request.getEndDate())) {
                break;
            }
            day = day.plusDays(1);
        }
        return ExecutionTrendResponse.builder().dailyPoints(points).build();
    }

    @Override
    public SkillParticipationResponse participation(SkillParticipationRequest request) {
        String key = request.getSortKey() == null ? "participantCount" : request.getSortKey();
        int topN = request.getTopN() == null ? DEFAULT_PARTICIPATION_TOP_N : request.getTopN();
        if (!Lists.newArrayList("participantCount", "participationRate", "assignedManagerCount").contains(key)) {
            throw invalid("技能参与度排序字段不支持");
        }
        ReportQuery query = query(request.getFilters());
        List<ReportRelation> relations = data.findRelations(query);
        Map<String, List<ReportRelation>> assigned = relations.stream().collect(Collectors.groupingBy(ReportRelation::getSkillId));
        Map<String, Set<String>> participants = Maps.newHashMap();
        for (ReportExecution execution : data.findExecutions(query, relations)) {
            if (execution.isRead()) {
                ReportRelation relation = execution.getRelation();
                participants.computeIfAbsent(relation.getSkillId(), id -> Sets.newHashSet()).add(relation.getSapId());
            }
        }
        List<SkillParticipationRow> rows = Lists.newArrayList();
        for (Map.Entry<String, List<ReportRelation>> entry : assigned.entrySet()) {
            Set<String> managers = entry.getValue().stream().map(ReportRelation::getSapId).collect(Collectors.toSet());
            long count = participants.getOrDefault(entry.getKey(), java.util.Collections.emptySet()).stream().filter(managers::contains).count();
            rows.add(SkillParticipationRow.builder().skillId(entry.getKey()).skillName(entry.getValue().get(0).getSkillName()).participantCount(count)
                    .assignedManagerCount(managers.size()).participationRate(ExecutionCalculations.rate(count, managers.size())).build());
        }
        Comparator<SkillParticipationRow> primary = participationComparator(key);
        rows.sort(primary.thenComparing(SkillParticipationRow::getSkillId));
        return SkillParticipationResponse.builder().rows(Lists.newArrayList(rows.subList(0, Math.min(topN, rows.size())))).build();
    }

    private Comparator<SkillParticipationRow> participationComparator(String key) {
        if ("participationRate".equals(key)) {
            return (left, right) -> ExecutionCalculations.compare(right.getParticipantCount(), right.getAssignedManagerCount(),
                    left.getParticipantCount(), left.getAssignedManagerCount());
        }
        return "assignedManagerCount".equals(key)
                ? Comparator.comparingLong(SkillParticipationRow::getAssignedManagerCount).reversed()
                : Comparator.comparingLong(SkillParticipationRow::getParticipantCount).reversed();
    }

    @Override
    public AttentionBranchesResponse attention(ReportSummaryRequest request) {
        ExecutionDataset dataset = load(request);
        ExecutionMetrics baseline = summarize(load(
                ReportSummaryRequest.builder().startDate(request.getStartDate()).endDate(request.getEndDate()).bbkId(request.getBbkId()).build()));
        Map<List<String>, List<ReportExecution>> groups = dataset.getExecutions().stream()
                .collect(Collectors.groupingBy(e -> Lists.newArrayList(e.getRelation().getBbkId(), e.getRelation().getOrgId())));
        List<AttentionBranchRow> rows = Lists.newArrayList();
        for (List<ReportExecution> group : groups.values()) {
            ExecutionMetrics current = summarize(dataset, group);
            boolean viewLow = below(current, baseline, "viewRate");
            boolean contactLow = below(current, baseline, "contactRate");
            if (viewLow || contactLow) {
                String diagnosis = viewLow && contactLow ? "bothLow" : (viewLow ? "usageLow" : "contactLow");
                rows.add(attentionRow(group.get(0).getRelation(), current, baseline, diagnosis));
            }
        }
        rows.sort(attentionComparator());
        return AttentionBranchesResponse.builder().totalCount(rows.size())
                .rows(Lists.newArrayList(rows.subList(0, Math.min(ATTENTION_LIMIT, rows.size())))).build();
    }

    private boolean below(ExecutionMetrics current, ExecutionMetrics baseline, String key) {
        long total = current.getStatistics().getRecommendedCustomerCount();
        long baseTotal = baseline.getStatistics().getRecommendedCustomerCount();
        return total > 0 && baseTotal > 0 && ExecutionCalculations.compare(value(current, key), total, value(baseline, key), baseTotal) < 0;
    }

    private AttentionBranchRow attentionRow(ReportRelation relation, ExecutionMetrics current, ExecutionMetrics baseline, String diagnosis) {
        ExecutionMetricsResponse metrics = response(current);
        ExecutionMetricsResponse base = response(baseline);
        return AttentionBranchRow.builder().bbkId(relation.getBbkId()).bbkName(relation.getBbkName()).orgId(relation.getOrgId())
                .orgName(relation.getOrgName()).diagnosis(diagnosis).recommendedCustomerCount(metrics.getRecommendedCustomerCount())
                .viewCustomerCount(metrics.getViewCustomerCount()).contactCustomerCount(metrics.getContactCustomerCount())
                .viewRate(metrics.getViewRate()).contactRate(metrics.getContactRate()).benchmarkViewRate(base.getViewRate())
                .benchmarkContactRate(base.getContactRate()).viewRateDifference(difference(current, baseline, "viewRate"))
                .contactRateDifference(difference(current, baseline, "contactRate")).build();
    }

    private Comparator<AttentionBranchRow> attentionComparator() {
        Comparator<AttentionBranchRow> primary = (left, right) -> {
            boolean leftContact = !"usageLow".equals(left.getDiagnosis());
            boolean rightContact = !"usageLow".equals(right.getDiagnosis());
            if (leftContact != rightContact) {
                return leftContact ? -1 : 1;
            }
            return ExecutionCalculations.compare(leftContact ? left.getContactCustomerCount() : left.getViewCustomerCount(),
                    left.getRecommendedCustomerCount(), rightContact ? right.getContactCustomerCount() : right.getViewCustomerCount(),
                    right.getRecommendedCustomerCount());
        };
        return primary.thenComparing(Comparator.comparingLong(AttentionBranchRow::getRecommendedCustomerCount).reversed())
                .thenComparing(AttentionBranchRow::getBbkId).thenComparing(AttentionBranchRow::getOrgId);
    }

    @Override
    public ExecutionOverviewResponse overview(ReportSummaryRequest request) {
        ExecutionMetrics metrics = summarize(load(request));
        ReportStatistics totals = metrics.getStatistics();
        return ExecutionOverviewResponse.builder().metrics(response(metrics)).managerCount(totals.getUserCount())
                .customersPerTask(ExecutionCalculations.divide(totals.getRecommendedCustomerCount(), totals.getTaskCount(), 1))
                .averageTaskCount(average(totals.getTaskCount(), totals.getUserCount()))
                .averageRecommendedCustomerCount(average(totals.getRecommendedCustomerCount(), totals.getUserCount()))
                .averageContactCustomerCount(average(totals.getContactedCustomerCount(), totals.getUserCount())).largestLoss(loss(metrics)).build();
    }

    private Long average(long count, long users) {
        BigDecimal value = ExecutionCalculations.divide(count, users, 0);
        return value == null ? null : value.longValueExact();
    }

    private ExecutionDataset load(ReportSummaryRequest request) {
        ReportQuery query = query(request);
        List<ReportExecution> executions = data.findExecutions(query, data.findRelations(query));
        List<ReportRecommendation> recommendations = data.findRecommendations(executions);
        return ExecutionDataset.builder().executions(executions).recommendations(recommendations).actions(data.findActions(recommendations)).build();
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

    private ExecutionMetrics summarize(ExecutionDataset dataset) {
        return statistics.summarize(dataset.getExecutions(), dataset.getRecommendations(), dataset.getActions());
    }

    private ExecutionMetrics summarize(ExecutionDataset dataset, List<ReportExecution> executions) {
        Set<ReportExecution> selected = Sets.newHashSet(executions);
        return statistics.summarize(executions,
                dataset.getRecommendations().stream().filter(r -> selected.contains(r.getExecution())).collect(Collectors.toList()),
                dataset.getActions().stream().filter(a -> selected.contains(a.getRecommendation().getExecution())).collect(Collectors.toList()));
    }

    private ExecutionMetricsResponse response(ExecutionMetrics metrics) {
        ReportStatistics s = metrics.getStatistics();
        long customers = s.getRecommendedCustomerCount();
        return ExecutionMetricsResponse.builder().taskCount(s.getTaskCount()).scheduledTaskCount(s.getScheduledTaskCount())
                .executedTaskCount(s.getExecutedTaskCount()).pendingTaskCount(s.getPendingTaskCount()).failedTaskCount(s.getFailedTaskCount())
                .recommendedCustomerCount(customers).viewCustomerCount(s.getViewedCustomerCount())
                .insightCustomerCount(metrics.getInsightCustomerCount()).callCustomerCount(metrics.getCallCustomerCount())
                .contactCustomerCount(s.getContactedCustomerCount()).viewRate(ExecutionCalculations.rate(s.getViewedCustomerCount(), customers))
                .insightRate(ExecutionCalculations.rate(metrics.getInsightCustomerCount(), customers))
                .callRate(ExecutionCalculations.rate(metrics.getCallCustomerCount(), customers))
                .contactRate(ExecutionCalculations.rate(s.getContactedCustomerCount(), customers)).build();
    }

    private ExecutionLossResponse loss(ExecutionMetrics metrics) {
        long[] counts = {metrics.getStatistics().getViewedCustomerCount(), metrics.getInsightCustomerCount(), metrics.getCallCustomerCount(),
                metrics.getStatistics().getContactedCustomerCount()};
        String[] stages = {"view", "insight", "call", "contact"};
        int best = -1;
        for (int index = 0; index < counts.length - 1; index++) {
            if (counts[index] > 0 && (best < 0 || ExecutionCalculations.compare(counts[index] - counts[index + 1], counts[index],
                    counts[best] - counts[best + 1], counts[best]) < 0)) {
                best = index;
            }
        }
        return best < 0
                ? null
                : ExecutionLossResponse.builder().fromStage(stages[best]).toStage(stages[best + 1])
                        .customerDifference(counts[best] - counts[best + 1])
                        .lossRate(ExecutionCalculations.rate(counts[best] - counts[best + 1], counts[best])).build();
    }

    @Override
    public ExecutionBranchesResponse branches(ExecutionBranchesRequest request) {
        String key = request.getSortKey() == null ? "taskCount" : request.getSortKey();
        String direction = request.getSortDir() == null ? "desc" : request.getSortDir();
        if (!SORT_KEYS.contains(key) || !Lists.newArrayList("asc", "desc").contains(direction)) {
            throw invalid("排序字段或方向不支持，请使用约定的排序字段及asc或desc");
        }
        ExecutionDataset dataset = load(request.getFilters());
        ReportSummaryRequest filters = request.getFilters();
        ExecutionMetrics baseline = summarize(load(
                ReportSummaryRequest.builder().startDate(filters.getStartDate()).endDate(filters.getEndDate()).bbkId(filters.getBbkId()).build()));
        Map<List<String>, List<ReportExecution>> groups = dataset.getExecutions().stream()
                .collect(Collectors.groupingBy(e -> Lists.newArrayList(e.getRelation().getBbkId(), e.getRelation().getOrgId())));
        Map<List<String>, ExecutionMetrics> metrics = Maps.newHashMap();
        List<ExecutionBranchRow> rows = Lists.newArrayList();
        for (Map.Entry<List<String>, List<ReportExecution>> entry : groups.entrySet()) {
            ExecutionMetrics current = summarize(dataset, entry.getValue());
            metrics.put(entry.getKey(), current);
            rows.add(branchRow(entry.getValue().get(0).getRelation(), current, baseline));
        }
        rows.sort(branchComparator(metrics, key, direction));
        ExecutionMetricsResponse base = response(baseline);
        return ExecutionBranchesResponse.builder().rows(rows).benchmark(ExecutionBenchmarkResponse.builder().viewRate(base.getViewRate())
                .insightRate(base.getInsightRate()).callRate(base.getCallRate()).contactRate(base.getContactRate()).build()).build();
    }

    private Comparator<ExecutionBranchRow> branchComparator(Map<List<String>, ExecutionMetrics> metrics, String key, String direction) {
        Comparator<ExecutionBranchRow> primary = (left, right) -> compare(metrics.get(Lists.newArrayList(left.getBbkId(), left.getOrgId())),
                metrics.get(Lists.newArrayList(right.getBbkId(), right.getOrgId())), key, direction);
        return primary.thenComparing(ExecutionBranchRow::getBbkId).thenComparing(ExecutionBranchRow::getOrgId);
    }

    private int compare(ExecutionMetrics left, ExecutionMetrics right, String key, String direction) {
        boolean rate = key.endsWith("Rate");
        long leftTotal = left.getStatistics().getRecommendedCustomerCount();
        long rightTotal = right.getStatistics().getRecommendedCustomerCount();
        if (rate && (leftTotal == 0 || rightTotal == 0)) {
            return Integer.compare(leftTotal == 0 ? 1 : 0, rightTotal == 0 ? 1 : 0);
        }
        int result = rate
                ? ExecutionCalculations.compare(value(left, key), leftTotal, value(right, key), rightTotal)
                : Long.compare(value(left, key), value(right, key));
        return "desc".equals(direction) ? -result : result;
    }

    private long value(ExecutionMetrics metrics, String key) {
        ReportStatistics s = metrics.getStatistics();
        long result = s.getContactedCustomerCount();
        if ("skillCount".equals(key)) {
            result = s.getSkillCount();
        } else if ("taskCount".equals(key)) {
            result = s.getTaskCount();
        } else if ("recommendedCustomerCount".equals(key)) {
            result = s.getRecommendedCustomerCount();
        } else if ("viewRate".equals(key)) {
            result = s.getViewedCustomerCount();
        } else if ("insightRate".equals(key)) {
            result = metrics.getInsightCustomerCount();
        } else if ("callRate".equals(key)) {
            result = metrics.getCallCustomerCount();
        }
        return result;
    }

    private BigDecimal difference(ExecutionMetrics current, ExecutionMetrics baseline, String key) {
        return ExecutionCalculations.difference(value(current, key), current.getStatistics().getRecommendedCustomerCount(), value(baseline, key),
                baseline.getStatistics().getRecommendedCustomerCount());
    }

    private String status(ExecutionMetrics current, ExecutionMetrics baseline, String key) {
        return ExecutionCalculations.status(value(current, key), current.getStatistics().getRecommendedCustomerCount(), value(baseline, key),
                baseline.getStatistics().getRecommendedCustomerCount());
    }

    private ExecutionBranchRow branchRow(ReportRelation relation, ExecutionMetrics current, ExecutionMetrics baseline) {
        return ExecutionBranchRow.builder().bbkId(relation.getBbkId()).bbkName(relation.getBbkName()).orgId(relation.getOrgId())
                .orgName(relation.getOrgName()).skillCount(current.getStatistics().getSkillCount()).metrics(response(current))
                .viewRateDifference(difference(current, baseline, "viewRate")).insightRateDifference(difference(current, baseline, "insightRate"))
                .callRateDifference(difference(current, baseline, "callRate")).contactRateDifference(difference(current, baseline, "contactRate"))
                .viewRateStatus(status(current, baseline, "viewRate")).insightRateStatus(status(current, baseline, "insightRate"))
                .callRateStatus(status(current, baseline, "callRate")).contactRateStatus(status(current, baseline, "contactRate")).build();
    }

    private ExecutionDataset scoped(ExecutionDetailsRequest request) {
        ReportSummaryRequest filters = request.getFilters();
        if (filters.getBbkId() != null && !filters.getBbkId().equals(request.getScopeBbkId())
                || filters.getOrgId() != null && !filters.getOrgId().equals(request.getScopeOrgId())) {
            throw invalid("下钻机构超出当前检索范围");
        }
        return load(ReportSummaryRequest.builder().startDate(filters.getStartDate()).endDate(filters.getEndDate()).bbkId(request.getScopeBbkId())
                .orgId(request.getScopeOrgId()).level(filters.getLevel()).skillIds(filters.getSkillIds()).build());
    }

    @Override
    public ExecutionManagerDetailsResponse managers(ExecutionDetailsRequest request) {
        ExecutionDataset dataset = scoped(request);
        ReportOrganization organization = data.findOrganization(request.getScopeBbkId(), request.getScopeOrgId());
        ExecutionMetrics current = summarize(dataset);
        ExecutionMetricsResponse total = response(current);
        ReportStatistics summary = current.getStatistics();
        Map<String, List<ReportExecution>> groups = dataset.getExecutions().stream().collect(Collectors.groupingBy(e -> e.getRelation().getSapId()));
        List<ExecutionManagerRow> rows = Lists.newArrayList();
        for (List<ReportExecution> group : groups.values()) {
            rows.add(managerRow(dataset, group));
        }
        rows.sort(Comparator.comparingLong(ExecutionManagerRow::getTaskCount).reversed().thenComparing(ExecutionManagerRow::getSapId));
        return ExecutionManagerDetailsResponse.builder().bbkId(request.getScopeBbkId()).orgId(request.getScopeOrgId())
                .bbkName(organization.getBbkName()).orgName(organization.getOrgName())
                .summary(ExecutionManagerSummary.builder().managerCount(summary.getUserCount()).taskCount(total.getTaskCount())
                        .scheduledTaskCount(total.getScheduledTaskCount()).executedTaskCount(total.getExecutedTaskCount())
                        .pendingTaskCount(total.getPendingTaskCount()).failedTaskCount(total.getFailedTaskCount())
                        .recommendedCustomerCount(total.getRecommendedCustomerCount()).contactRate(total.getContactRate()).build())
                .rows(rows).build();
    }

    private ExecutionManagerRow managerRow(ExecutionDataset dataset, List<ReportExecution> executions) {
        ReportRelation first = executions.get(0).getRelation();
        ExecutionMetricsResponse metrics = response(summarize(dataset, executions));
        Map<String, ExecutionSkillOption> skills = executions.stream().map(ReportExecution::getRelation)
                .collect(Collectors.toMap(ReportRelation::getSkillId,
                        r -> ExecutionSkillOption.builder().skillId(r.getSkillId()).skillName(r.getSkillName()).build(), (a, b) -> a,
                        Maps::newTreeMap));
        return ExecutionManagerRow.builder().sapId(first.getSapId()).userName(first.getUserName()).level(first.getLevel())
                .skills(Lists.newArrayList(skills.values())).taskCount(metrics.getTaskCount()).scheduledTaskCount(metrics.getScheduledTaskCount())
                .executedTaskCount(metrics.getExecutedTaskCount()).pendingTaskCount(metrics.getPendingTaskCount())
                .failedTaskCount(metrics.getFailedTaskCount()).recommendedCustomerCount(metrics.getRecommendedCustomerCount())
                .viewRate(metrics.getViewRate()).insightRate(metrics.getInsightRate()).callRate(metrics.getCallRate())
                .contactRate(metrics.getContactRate()).build();
    }

    @Override
    public ExecutionSkillDetailsResponse skills(ExecutionDetailsRequest request) {
        ExecutionDataset dataset = scoped(request);
        ReportOrganization organization = data.findOrganization(request.getScopeBbkId(), request.getScopeOrgId());
        ReportStatistics summary = summarize(dataset).getStatistics();
        Map<String, List<ReportExecution>> groups = dataset.getExecutions().stream()
                .collect(Collectors.groupingBy(e -> e.getRelation().getSkillId()));
        Map<String, String> categories = data.findSkillCategories(Lists.newArrayList(groups.keySet()));
        List<ExecutionSkillRow> rows = Lists.newArrayList();
        for (List<ReportExecution> group : groups.values()) {
            rows.add(skillRow(dataset, group, categories.get(group.get(0).getRelation().getSkillId())));
        }
        rows.sort(Comparator.comparingLong(ExecutionSkillRow::getTaskCount).reversed().thenComparing(ExecutionSkillRow::getSkillId));
        return ExecutionSkillDetailsResponse.builder().bbkId(request.getScopeBbkId()).orgId(request.getScopeOrgId())
                .bbkName(organization.getBbkName()).orgName(organization.getOrgName())
                .summary(ExecutionSkillSummary.builder().skillCount(summary.getSkillCount()).managerCount(summary.getUserCount())
                        .taskCount(summary.getTaskCount()).scheduledTaskCount(summary.getScheduledTaskCount())
                        .executedTaskCount(summary.getExecutedTaskCount()).pendingTaskCount(summary.getPendingTaskCount())
                        .failedTaskCount(summary.getFailedTaskCount()).planCount(summary.getPlanCount()).build())
                .rows(rows).build();
    }

    private ExecutionSkillRow skillRow(ExecutionDataset dataset, List<ReportExecution> executions, String categoryName) {
        ReportRelation first = executions.get(0).getRelation();
        ExecutionMetrics current = summarize(dataset, executions);
        ReportStatistics summary = current.getStatistics();
        ExecutionMetricsResponse metrics = response(current);
        return ExecutionSkillRow.builder().skillId(first.getSkillId()).skillName(first.getSkillName()).categoryName(categoryName)
                .managerCount(summary.getUserCount()).taskCount(summary.getTaskCount()).scheduledTaskCount(summary.getScheduledTaskCount())
                .executedTaskCount(summary.getExecutedTaskCount()).pendingTaskCount(summary.getPendingTaskCount())
                .failedTaskCount(summary.getFailedTaskCount()).recommendedCustomerCount(summary.getRecommendedCustomerCount())
                .viewRate(metrics.getViewRate()).contactRate(metrics.getContactRate()).planCount(summary.getPlanCount()).build();
    }
}
