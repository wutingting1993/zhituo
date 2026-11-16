package com.cmbchina.application.modelcontext.support;
import com.cmbchina.application.modelcontext.api.dto.ModelKnowledge;
import com.cmbchina.application.modelcontext.api.dto.ModelCondition;
import com.cmbchina.application.modelcontext.api.dto.ModelIssue;
import com.cmbchina.domain.knowledge.model.KnowledgeItem;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.Map;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;
/** 按稳定知识ID组织正文和条件。 */
@Component
public class ModelKnowledgeAssembler {
    /** 聚合当前显式适用关系。 */
    public Map<String, ModelKnowledge> collect(List<KnowledgeItem> rows, List<ModelIssue> issues) {
        Map<String, ModelKnowledge> entries = Maps.newLinkedHashMap();
        for (KnowledgeItem row : rows) {
            if (!"BUSINESS".equals(row.getKnowledgeType()) && !"TECHNICAL".equals(row.getKnowledgeType())) {
                issue(issues, "KNOWLEDGE_TYPE_MISSING", row.getKnowledgeId());
                continue;
            }
            ModelKnowledge entry = entries.get(row.getKnowledgeId());
            if (entry == null) {
                entry = full(row);
                entry.setConditions(Lists.newArrayList());
                entries.put(entry.getId(), entry);
            }
            condition(entry, row);
            if ("PENDING".equals(row.getStatus())) {
                issue(issues, "KNOWLEDGE_PENDING", row.getKnowledgeId());
            }
            if (!row.isUnconditional() && (row.getConditionText() == null || row.getConditionText().trim().isEmpty())) {
                issue(issues, "CONDITION_UNSPECIFIED", row.getId());
            }
            if ("EXPIRED".equals(row.getReplacesStatus())) {
                issue(issues, "EXPIRED_KNOWLEDGE", row.getReplacesKnowledgeId());
            }
        }
        return entries;
    }

    /** 从正文行创建知识，不推断条件。 */
    public ModelKnowledge full(KnowledgeItem row) {
        return ModelKnowledge.builder().id(row.getKnowledgeId()).title(row.getTitle()).content(row.getContent()).status(row.getStatus()).build();
    }

    private void condition(ModelKnowledge entry, KnowledgeItem row) {
        boolean exists = entry.getConditions().stream()
                .anyMatch(c -> c.isUnconditional() == row.isUnconditional() && Objects.equals(c.getCondition(), row.getConditionText())
                        && Objects.equals(c.getException(), row.getExceptionText())
                        && Objects.equals(c.getReplacesKnowledgeId(), row.getReplacesKnowledgeId()));
        if (!exists) {
            entry.getConditions().add(ModelCondition.builder().unconditional(row.isUnconditional()).condition(row.getConditionText())
                    .exception(row.getExceptionText()).replacesKnowledgeId(row.getReplacesKnowledgeId()).build());
        }
    }

    /** 去重缺口，不改变正文或确认状态。 */
    public List<ModelIssue> issues(List<ModelIssue> rows) {
        Map<String, ModelIssue> unique = Maps.newLinkedHashMap();
        for (ModelIssue row : rows) {
            unique.putIfAbsent(row.getType() + ":" + row.getTarget(), row);
        }
        return Lists.newArrayList(unique.values());
    }

    private void issue(List<ModelIssue> issues, String type, String target) {
        issues.add(ModelIssue.builder().type(type).target(target).build());
    }
}