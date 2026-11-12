package com.cmbchina.infrastructure.report.supportimpl;
import com.cmbchina.domain.report.model.ReportOrganization;
import com.cmbchina.infrastructure.report.dao.po.ReportSkillCategoryPo;
import com.cmbchina.infrastructure.report.dao.po.ReportOrganizationPo;
import com.cmbchina.domain.report.model.ReportCustomerAction;
import com.cmbchina.infrastructure.report.dao.po.ReportActionPo;
import com.cmbchina.domain.report.model.ReportPlan;
import com.cmbchina.infrastructure.report.dao.po.ReportPlanPo;
import com.cmbchina.domain.report.model.ReportQuery;
import com.cmbchina.domain.report.model.ReportRelation;
import com.cmbchina.domain.report.model.ReportExecution;
import com.cmbchina.domain.report.model.ReportRecommendation;
import com.cmbchina.domain.report.model.ReportContact;
import com.cmbchina.domain.report.support.ReportDataSupport;
import com.cmbchina.infrastructure.report.dao.mapper.ReportMapper;
import com.cmbchina.infrastructure.report.dao.converter.ReportDataConverter;
import com.cmbchina.infrastructure.report.dao.po.ReportTargetPo;
import com.cmbchina.infrastructure.report.dao.po.ReportJobPo;
import com.cmbchina.infrastructure.report.dao.po.ReportPlanSkillPo;
import com.cmbchina.infrastructure.report.dao.po.ReportSkillOptionPo;
import com.cmbchina.infrastructure.report.dao.po.ReportExecutionPo;
import com.cmbchina.infrastructure.report.dao.po.ReportResultPo;
import com.cmbchina.infrastructure.report.dao.po.ReportContactPo;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
/** 分批读取并按完整业务关系匹配。 */
@Component
public class ReportDataSupportImpl implements ReportDataSupport {
    private static final int BATCH_SIZE = 200;
    @Autowired
    private ReportMapper mapper;

    @Override
    public List<ReportOrganization> findFilterOrganizations() {
        return mapper.filterOrganizations().stream().map(row -> ReportOrganization.builder().bbkId(row.getBbkId()).bbkName(row.getBbkName())
                .orgId(row.getOrgId()).orgName(row.getOrgName()).build()).collect(Collectors.toList());
    }

    @Override
    public List<com.cmbchina.domain.report.model.ReportSkillOption> findFilterSkills(String bbkId, String orgId) {
        Map<String, ReportSkillOptionPo> skills = Maps.newTreeMap();
        if (bbkId == null && orgId == null) {
            for (ReportSkillOptionPo row : mapper.filterSkills()) {
                skills.put(row.getId(), row);
            }
        } else {
            for (List<ReportPlanSkillPo> batch : Lists.partition(mapper.filterPlanSkills(bbkId, orgId), BATCH_SIZE)) {
                for (ReportSkillOptionPo row : mapper.filterTaskSkills(batch)) {
                    skills.merge(row.getId(), row, this::skillName);
                }
            }
        }
        return skills.values().stream()
                .map(row -> com.cmbchina.domain.report.model.ReportSkillOption.builder().id(row.getId()).name(row.getName()).build())
                .collect(Collectors.toList());
    }

    private ReportSkillOptionPo skillName(ReportSkillOptionPo left, ReportSkillOptionPo right) {
        if (left.getName() == null) {
            return right;
        }
        if (right.getName() == null) {
            return left;
        }
        return left.getName().compareTo(right.getName()) <= 0 ? left : right;
    }

    @Override
    public List<String> findFilterLevels() {
        return mapper.filterLevels();
    }

    @Override
    public List<ReportPlan> findPlans(List<String> ids) {
        List<ReportPlan> result = Lists.newArrayList();
        for (List<String> batch : Lists.partition(ids, BATCH_SIZE)) {
            for (ReportPlanPo row : mapper.plans(batch)) {
                result.add(ReportDataConverter.plan(row));
            }
        }
        return result;
    }

    @Override
    public Map<String, String> findSkillCategories(List<String> skillIds) {
        Map<String, String> names = Maps.newHashMap();
        List<String> unique = skillIds.stream().distinct().collect(Collectors.toList());
        for (List<String> batch : Lists.partition(unique, BATCH_SIZE)) {
            for (ReportSkillCategoryPo row : mapper.skillCategories(batch)) {
                if (row.getCategoryName() != null) {
                    names.put(row.getSkillId(), row.getCategoryName());
                }
            }
        }
        return java.util.Collections.unmodifiableMap(names);
    }

    @Override
    public ReportOrganization findOrganization(String bbkId, String orgId) {
        ReportOrganizationPo row = mapper.organization(bbkId, orgId);
        if (row == null) {
            return ReportOrganization.builder().bbkId(bbkId).orgId(orgId).build();
        }
        return ReportOrganization.builder().bbkId(row.getBbkId()).bbkName(row.getBbkName()).orgId(row.getOrgId()).orgName(row.getOrgName()).build();
    }

    @Override
    public List<ReportRelation> findRelations(ReportQuery query) {
        List<ReportRelation> result = Lists.newArrayList();
        Set<String> skills = Sets.newHashSet(query.getSkillIds());
        for (List<ReportPlanSkillPo> scheduled : Lists.partition(mapper.scheduledSkills(query), BATCH_SIZE)) {
            List<String> planIds = scheduled.stream().map(ReportPlanSkillPo::getPlanId).distinct().collect(Collectors.toList());
            addRelations(query, scheduled, planIds, skills, result);
        }
        return result;
    }

    private void addRelations(ReportQuery query, List<ReportPlanSkillPo> scheduled, List<String> planIds, Set<String> skills,
            List<ReportRelation> result) {
        for (List<ReportTargetPo> batch : Lists.partition(mapper.targets(query, planIds), BATCH_SIZE)) {
            Map<List<String>, List<ReportJobPo>> jobs = loadJobs(batch, skills, scheduled).stream()
                    .collect(Collectors.groupingBy(j -> Lists.newArrayList(j.getPlanId(), j.getSapId())));
            for (ReportTargetPo target : batch) {
                List<ReportJobPo> matches = jobs.get(Lists.newArrayList(target.getPlanId(), target.getSapId()));
                if (matches != null) {
                    for (ReportJobPo job : matches) {
                        result.add(ReportDataConverter.relation(target, job));
                    }
                }
            }
        }
    }

    private List<ReportJobPo> loadJobs(List<ReportTargetPo> targets, Set<String> skills, List<ReportPlanSkillPo> scheduled) {
        if (skills.isEmpty()) {
            return mapper.jobs(targets, Lists.newArrayList(), scheduled);
        }
        List<ReportJobPo> result = Lists.newArrayList();
        for (List<String> batch : Lists.partition(Lists.newArrayList(skills), BATCH_SIZE)) {
            result.addAll(mapper.jobs(targets, batch, scheduled));
        }
        return result;
    }

    @Override
    public List<ReportExecution> findExecutions(ReportQuery query, List<ReportRelation> relations) {
        Map<List<String>, ReportExecutionPo> latest = Maps.newHashMap();
        for (List<ReportRelation> batch : Lists.partition(relations, BATCH_SIZE)) {
            for (ReportExecutionPo row : mapper.candidates(batch, query)) {
                List<String> key = Lists.newArrayList(row.getJobId(), row.getActualTime().toLocalDate().toString());
                ReportExecutionPo previous = latest.get(key);
                if (previous == null || row.getActualTime().isAfter(previous.getActualTime())) {
                    latest.put(key, row);
                }
            }
        }
        Map<String, List<ReportExecutionPo>> byTrace = loadTraces(latest);
        List<ReportExecution> result = Lists.newArrayList();
        Map<String, List<ReportExecutionPo>> byJob = latest.values().stream().collect(Collectors.groupingBy(ReportExecutionPo::getJobId));
        for (ReportRelation relation : relations) {
            addExecutions(relation, byJob, byTrace, result);
        }
        addPending(query, relations, result);
        return result;
    }

    private void addPending(ReportQuery query, List<ReportRelation> relations, List<ReportExecution> result) {
        Set<List<String>> executed = result.stream().map(e -> taskKey(e.getRelation(), e.getExecutionDay())).collect(Collectors.toSet());
        Map<List<String>, Set<java.time.LocalDate>> days = Maps.newHashMap();
        Map<List<String>, ReportRelation> unique = Maps.newHashMap();
        for (ReportRelation relation : relations) {
            unique.putIfAbsent(Lists.newArrayList(relation.getPlanId(), relation.getSkillId()), relation);
        }
        for (List<ReportRelation> batch : Lists.partition(Lists.newArrayList(unique.values()), BATCH_SIZE)) {
            for (ReportPlanSkillPo row : mapper.scheduledDays(query, batch)) {
                days.computeIfAbsent(Lists.newArrayList(row.getPlanId(), row.getSkillId()), key -> Sets.newHashSet()).add(row.getExecutionDay());
            }
        }
        Set<List<String>> added = Sets.newHashSet(executed);
        for (ReportRelation relation : relations) {
            for (java.time.LocalDate day : days.getOrDefault(Lists.newArrayList(relation.getPlanId(), relation.getSkillId()),
                    java.util.Collections.emptySet())) {
                if (added.add(taskKey(relation, day))) {
                    result.add(ReportExecution.builder().relation(relation).executionDay(day).build());
                }
            }
        }
    }

    private List<String> taskKey(ReportRelation relation, java.time.LocalDate day) {
        return Lists.newArrayList(relation.getSapId(), relation.getPlanId(), relation.getSkillId(), day.toString(), relation.getJobId());
    }

    private Map<String, List<ReportExecutionPo>> loadTraces(Map<List<String>, ReportExecutionPo> latest) {
        List<ReportExecutionPo> selected = latest.values().stream().filter(row -> row.getTraceId() != null).collect(Collectors.toList());
        Map<String, List<ReportExecutionPo>> result = Maps.newHashMap();
        for (List<ReportExecutionPo> batch : Lists.partition(selected, BATCH_SIZE)) {
            for (ReportExecutionPo row : mapper.executions(batch)) {
                result.computeIfAbsent(row.getTraceId(), key -> Lists.newArrayList()).add(row);
            }
        }
        return result;
    }

    private void addExecutions(ReportRelation relation, Map<String, List<ReportExecutionPo>> byJob, Map<String, List<ReportExecutionPo>> byTrace,
            List<ReportExecution> result) {
        List<ReportExecutionPo> selectedRows = byJob.get(relation.getJobId());
        if (selectedRows == null) {
            return;
        }
        for (ReportExecutionPo selected : selectedRows) {
            if (selected.getTraceId() == null) {
                ReportExecution execution = ReportDataConverter.execution(relation, selected);
                execution.setTaskStatus(selected.getStatus());
                result.add(execution);
                continue;
            }
            List<ReportExecutionPo> records = byTrace.get(selected.getTraceId());
            if (records != null) {
                for (ReportExecutionPo row : records) {
                    if (relation.getJobId().equals(row.getJobId()) && selected.getActualTime().toLocalDate().equals(row.getActualTime().toLocalDate())
                            && !row.getActualTime().isBefore(relation.getCreatedAt())) {
                        ReportExecution execution = ReportDataConverter.execution(relation, row);
                        execution.setTaskStatus(selected.getStatus());
                        result.add(execution);
                    }
                }
            }
        }
    }

    @Override
    public List<ReportRecommendation> findRecommendations(List<ReportExecution> executions) {
        Map<List<String>, List<ReportExecution>> byKey = executions.stream().filter(e -> e.getTraceId() != null)
                .collect(Collectors.groupingBy(e -> Lists.newArrayList(e.getRelation().getJobId(), e.getRelation().getSkillId(), e.getTraceId())));
        List<ReportRecommendation> result = Lists.newArrayList();
        List<ReportExecution> unique = byKey.values().stream().map(rows -> rows.get(0)).collect(Collectors.toList());
        for (List<ReportExecution> batch : Lists.partition(unique, BATCH_SIZE)) {
            for (ReportResultPo row : mapper.results(batch)) {
                List<ReportExecution> matches = byKey.get(Lists.newArrayList(row.getJobId(), row.getSkillId(), row.getTraceId()));
                if (matches != null) {
                    for (ReportExecution execution : matches) {
                        result.add(ReportRecommendation.builder().execution(execution).customerId(row.getCustomerId()).build());
                    }
                }
            }
        }
        return result;
    }

    @Override
    public List<ReportCustomerAction> findActions(List<ReportRecommendation> recommendations) {
        Map<List<String>, List<ReportRecommendation>> matches = recommendations.stream().collect(Collectors.groupingBy(this::actionKey));
        List<ReportRecommendation> unique = matches.values().stream().map(rows -> rows.get(0)).collect(Collectors.toList());
        List<ReportCustomerAction> result = Lists.newArrayList();
        for (List<ReportRecommendation> batch : Lists.partition(unique, BATCH_SIZE)) {
            for (ReportActionPo row : mapper.actions(batch)) {
                List<String> key = Lists.newArrayList(row.getTraceId(), row.getCustomerId(), row.getExecutionDay().toString());
                for (ReportRecommendation recommendation : matches.get(key)) {
                    result.add(ReportCustomerAction.builder().recommendation(recommendation).buttonType(row.getButtonType())
                            .contactCustomerId(row.getContactCustomerId()).build());
                }
            }
        }
        return result;
    }

    private List<String> actionKey(ReportRecommendation row) {
        return Lists.newArrayList(row.getExecution().getTraceId(), row.getCustomerId(), row.getExecution().getExecutionDay().toString());
    }

    @Override
    public List<ReportContact> findContacts(List<ReportRecommendation> recommendations) {
        List<ReportContact> result = Lists.newArrayList();
        for (List<ReportRecommendation> batch : Lists.partition(recommendations, BATCH_SIZE)) {
            for (ReportContactPo row : mapper.contacts(batch)) {
                result.add(ReportContact.builder().customerId(row.getCustomerId()).build());
            }
        }
        return result;
    }
}
