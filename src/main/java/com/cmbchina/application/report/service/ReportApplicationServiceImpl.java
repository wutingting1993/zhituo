package com.cmbchina.application.report.service;
import com.cmbchina.application.report.api.ReportApplicationService;
import com.cmbchina.application.report.api.dto.ReportFilterRequest;
import com.cmbchina.application.report.api.dto.ReportFiltersResponse;
import com.cmbchina.application.report.api.dto.ReportSummaryRequest;
import com.cmbchina.application.report.api.dto.ReportSummaryResponse;
import com.cmbchina.application.report.api.dto.ReportOption;
import com.cmbchina.application.report.api.dto.MostUsedSkillResponse;
import com.cmbchina.domain.report.model.ReportQuery;
import com.cmbchina.domain.report.model.ReportRelation;
import com.cmbchina.domain.report.model.ReportExecution;
import com.cmbchina.domain.report.model.ReportRecommendation;
import com.cmbchina.domain.report.model.ReportStatistics;
import com.cmbchina.domain.report.model.MostUsedSkill;
import com.cmbchina.domain.report.service.ReportStatisticsService;
import com.cmbchina.domain.report.support.ReportDataSupport;
import com.cmbchina.infrastructure.common.ResponseEnum;
import com.cmbchina.infrastructure.common.exception.BusinessException;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
/** 报表过滤、统计及机构基准用例。 */
@Service
public class ReportApplicationServiceImpl implements ReportApplicationService {
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    @Autowired
    private ReportDataSupport data;
    @Autowired
    private ReportStatisticsService statistics;

    @Override
    public ReportFiltersResponse filters(ReportFilterRequest request) {
        validateOrganization(request.getBbkId(), request.getOrgId());
        ReportQuery branches = ReportQuery.builder().orgId(request.getOrgId()).skillIds(request.getSkillIds()).build();
        ReportQuery organizations = ReportQuery.builder().bbkId(request.getBbkId()).skillIds(request.getSkillIds()).build();
        ReportQuery skills = ReportQuery.builder().bbkId(request.getBbkId()).orgId(request.getOrgId()).build();
        List<ReportRelation> levels = data
                .findRelations(ReportQuery.builder().bbkId(request.getBbkId()).orgId(request.getOrgId()).skillIds(request.getSkillIds()).build());
        return ReportFiltersResponse.builder().branches(options(data.findRelations(branches), ReportRelation::getBbkId, ReportRelation::getBbkName))
                .subBranches(options(data.findRelations(organizations), ReportRelation::getOrgId, ReportRelation::getOrgName))
                .skills(options(data.findRelations(skills), ReportRelation::getSkillId, ReportRelation::getSkillName))
                .levels(levels.stream().map(ReportRelation::getLevel).filter(Objects::nonNull).distinct().sorted().collect(Collectors.toList()))
                .build();
    }

    private List<ReportOption> options(List<ReportRelation> rows, Function<ReportRelation, String> id, Function<ReportRelation, String> name) {
        Map<String, ReportOption> values = Maps.newTreeMap();
        for (ReportRelation row : rows) {
            String key = id.apply(row);
            values.putIfAbsent(key, ReportOption.builder().id(key).name(name.apply(row)).build());
        }
        return Lists.newArrayList(values.values());
    }

    @Override
    public ReportSummaryResponse summary(ReportSummaryRequest request) {
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new BusinessException(ResponseEnum.VALIDATION_ERROR.getCode(), "开始日期不能晚于结束日期", null);
        }
        validateOrganization(request.getBbkId(), request.getOrgId());
        ReportQuery query = ReportQuery.builder().bbkId(request.getBbkId()).orgId(request.getOrgId()).level(request.getLevel())
                .skillIds(request.getSkillIds()).startDate(request.getStartDate()).endDate(request.getEndDate()).build();
        ReportQuery benchmark = ReportQuery.builder().bbkId(request.getBbkId()).startDate(request.getStartDate()).endDate(request.getEndDate())
                .build();
        return response(summarize(query), summarize(benchmark));
    }

    private void validateOrganization(String bbkId, String orgId) {
        if (bbkId != null && orgId != null && !data.matchesOrganization(bbkId, orgId)) {
            throw new BusinessException(ResponseEnum.VALIDATION_ERROR.getCode(), "分行与支行不匹配", null);
        }
    }

    private ReportStatistics summarize(ReportQuery query) {
        List<ReportExecution> executions = data.findExecutions(query, data.findRelations(query));
        List<ReportRecommendation> recommendations = data.findRecommendations(executions);
        return statistics.summarize(executions, recommendations, data.findContacts(recommendations));
    }

    private ReportSummaryResponse response(ReportStatistics current, ReportStatistics benchmark) {
        MostUsedSkill skill = current.getMostUsedSkill();
        MostUsedSkillResponse mostUsed = skill == null
                ? null
                : MostUsedSkillResponse.builder().id(skill.getId()).name(skill.getName()).userCount(skill.getUserCount()).build();
        return ReportSummaryResponse.builder().planCount(current.getPlanCount()).skillCount(current.getSkillCount()).taskCount(current.getTaskCount())
                .recommendedCustomerCount(current.getRecommendedCustomerCount()).coveredSubBranchCount(current.getCoveredSubBranchCount())
                .mostUsedSkill(mostUsed).averageTaskCount(average(current.getTaskCount(), current.getUserCount()))
                .averageRecommendedCustomerCount(average(current.getRecommendedCustomerCount(), current.getUserCount()))
                .viewRate(rate(current.getViewedCustomerCount(), current.getRecommendedCustomerCount()))
                .contactRate(rate(current.getContactedCustomerCount(), current.getRecommendedCustomerCount()))
                .benchmarkViewRate(rate(benchmark.getViewedCustomerCount(), benchmark.getRecommendedCustomerCount()))
                .benchmarkContactRate(rate(benchmark.getContactedCustomerCount(), benchmark.getRecommendedCustomerCount()))
                .viewRateDifference(difference(current.getViewedCustomerCount(), current, benchmark.getViewedCustomerCount(), benchmark))
                .contactRateDifference(difference(current.getContactedCustomerCount(), current, benchmark.getContactedCustomerCount(), benchmark))
                .build();
    }

    private Long average(long count, long users) {
        return users == 0 ? null : BigDecimal.valueOf(count).divide(BigDecimal.valueOf(users), 0, RoundingMode.HALF_UP).longValueExact();
    }

    private BigDecimal rate(long count, long customers) {
        return customers == 0 ? null : BigDecimal.valueOf(count).multiply(HUNDRED).divide(BigDecimal.valueOf(customers), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal difference(long currentCount, ReportStatistics current, long benchmarkCount, ReportStatistics benchmark) {
        long currentCustomers = current.getRecommendedCustomerCount();
        long benchmarkCustomers = benchmark.getRecommendedCustomerCount();
        if (currentCustomers == 0 || benchmarkCustomers == 0) {
            return null;
        }
        BigDecimal numerator = BigDecimal.valueOf(currentCount).multiply(BigDecimal.valueOf(benchmarkCustomers))
                .subtract(BigDecimal.valueOf(benchmarkCount).multiply(BigDecimal.valueOf(currentCustomers))).multiply(HUNDRED);
        BigDecimal denominator = BigDecimal.valueOf(currentCustomers).multiply(BigDecimal.valueOf(benchmarkCustomers));
        return numerator.divide(denominator, 2, RoundingMode.HALF_UP);
    }
}
