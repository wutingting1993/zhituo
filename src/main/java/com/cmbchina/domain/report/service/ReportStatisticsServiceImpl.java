package com.cmbchina.domain.report.service;
import com.cmbchina.domain.report.model.ReportExecution;
import com.cmbchina.domain.report.model.ReportRecommendation;
import com.cmbchina.domain.report.model.ReportContact;
import com.cmbchina.domain.report.model.ReportStatistics;
import com.cmbchina.domain.report.model.ReportRelation;
import com.cmbchina.domain.report.model.MostUsedSkill;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Objects;
import java.util.Comparator;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
/** 按需求的编号与组合键统一去重。 */
@Service
public class ReportStatisticsServiceImpl implements ReportStatisticsService {
    @Override
    public ReportStatistics summarize(List<ReportExecution> executions, List<ReportRecommendation> recommendations, List<ReportContact> contacts) {
        List<ReportRelation> relations = executions.stream().map(ReportExecution::getRelation).collect(Collectors.toList());
        Set<String> customers = Sets.newHashSet();
        Set<String> viewed = Sets.newHashSet();
        Set<String> listTasks = Sets.newHashSet();
        Set<List<String>> planTasks = Sets.newHashSet();
        for (ReportRecommendation row : recommendations) {
            String customer = row.getCustomerId();
            String trace = row.getExecution().getTraceId();
            if (customer.isEmpty()) {
                listTasks.add(trace);
            } else {
                planTasks.add(Lists.newArrayList(trace, customer));
                customers.add(customer);
                if (row.getExecution().isRead()) {
                    viewed.add(customer);
                }
            }
        }
        long contacted = contacts.stream().map(ReportContact::getCustomerId).filter(Objects::nonNull).distinct().count();
        return ReportStatistics.builder().planCount(relations.stream().map(ReportRelation::getPlanId).distinct().count())
                .skillCount(relations.stream().map(ReportRelation::getSkillId).distinct().count())
                .taskCount((long) listTasks.size() + planTasks.size()).recommendedCustomerCount(customers.size()).viewedCustomerCount(viewed.size())
                .contactedCustomerCount(contacted).userCount(relations.stream().map(ReportRelation::getSapId).distinct().count())
                .coveredSubBranchCount(relations.stream().map(r -> Lists.newArrayList(r.getBbkId(), r.getOrgId())).distinct().count())
                .mostUsedSkill(mostUsed(relations)).build();
    }

    private MostUsedSkill mostUsed(List<ReportRelation> relations) {
        Map<String, List<ReportRelation>> skills = relations.stream().collect(Collectors.groupingBy(ReportRelation::getSkillId));
        return skills.entrySet().stream()
                .map(entry -> MostUsedSkill.builder().id(entry.getKey()).name(entry.getValue().get(0).getSkillName())
                        .userCount(entry.getValue().stream().map(ReportRelation::getSapId).distinct().count()).build())
                .sorted(Comparator.comparingLong(MostUsedSkill::getUserCount).reversed().thenComparing(MostUsedSkill::getId)).findFirst()
                .orElse(null);
    }
}
