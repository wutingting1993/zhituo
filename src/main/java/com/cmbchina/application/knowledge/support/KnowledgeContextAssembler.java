package com.cmbchina.application.knowledge.support;
import com.cmbchina.application.knowledge.api.dto.RelatedComponent;
import com.cmbchina.application.knowledge.api.dto.RelatedComponentGroup;
import com.cmbchina.application.knowledge.api.dto.KnowledgeCondition;
import com.cmbchina.application.knowledge.api.dto.ComponentKnowledgeResponse;
import com.cmbchina.application.knowledge.api.dto.InterfaceContext;
import com.cmbchina.application.knowledge.api.dto.InterfaceError;
import com.cmbchina.application.knowledge.api.dto.InterfaceExample;
import com.cmbchina.application.knowledge.api.dto.InterfaceRule;
import com.cmbchina.application.knowledge.api.dto.KnowledgeIssue;
import com.cmbchina.application.knowledge.api.dto.KnowledgeItemResponse;
import com.cmbchina.domain.knowledge.model.ContractExample;
import com.cmbchina.domain.knowledge.model.ContractError;
import com.cmbchina.domain.knowledge.model.ContractField;
import com.cmbchina.domain.knowledge.model.KnowledgeItem;
import com.cmbchina.domain.knowledge.model.KnowledgeQuery;
import com.cmbchina.domain.knowledge.support.KnowledgeDataSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
/** 组装结构化接口契约及精简适用关系。 */
@Component
public class KnowledgeContextAssembler {
    @Autowired
    private KnowledgeDataSupport support;
    @Autowired
    private InterfaceFieldAssembler fields;
    @Autowired
    private ObjectMapper mapper;
    /** 补充命中接口的契约。 */
    public void fill(KnowledgeQuery query, ComponentKnowledgeResponse response) {
        if (query.getInterfaceIds().isEmpty()) {
            return;
        }
        List<ContractField> rows = support.fields(query);
        List<ContractExample> examples = support.examples(query);
        List<ContractError> errors = support.errors(query);
        for (InterfaceContext context : response.getInterfaceContexts()) {
            if (context.getInterfaceId() == null) {
                continue;
            }
            fields.fill(context, rows, response.getIssues());
            if (context.getInputs().isEmpty() && context.getOutputs().isEmpty()) {
                issue(response, "INTERFACE_CONTRACT_MISSING", context.getInterfaceId());
            }
            fillExamples(context, examples, response);
            fillErrors(context, errors);
            fillRules(context, response);
        }
    }

    /** 保存当前条件并返回其他组件的显式引用，不推断全系统代码影响。 */
    public void related(KnowledgeQuery query, ComponentKnowledgeResponse response) {
        Map<String, KnowledgeItemResponse> entries = Maps.newLinkedHashMap();
        for (KnowledgeItemResponse entry : response.getBusinessKnowledge()) {
            entries.put(entry.getId(), entry);
        }
        for (KnowledgeItemResponse entry : response.getTechnicalKnowledge()) {
            entries.put(entry.getId(), entry);
        }
        for (RelatedComponentGroup group : response.getRelatedComponents()) {
            remember(entries.get(group.getKnowledgeId()), group.getItems());
        }
        query.setKnowledgeIds(com.google.common.collect.Lists.newArrayList(entries.keySet()));
        query.setCurrentComponentId(response.getComponent() == null ? null : response.getComponent().getId());
        Map<String, RelatedComponentGroup> groups = Maps.newLinkedHashMap();
        for (KnowledgeItem row : support.relatedComponents(query)) {
            RelatedComponentGroup group = groups.get(row.getKnowledgeId());
            if (group == null) {
                group = RelatedComponentGroup.builder().knowledgeId(row.getKnowledgeId()).build();
                groups.put(row.getKnowledgeId(), group);
            }
            group.getItems().add(relation(row));
        }
        response.setRelatedComponents(com.google.common.collect.Lists.newArrayList(groups.values()));
    }

    private void remember(KnowledgeItemResponse entry, List<RelatedComponent> items) {
        for (RelatedComponent item : items) {
            boolean exists = entry.getConditions().stream()
                    .anyMatch(value -> value.isUnconditional() == item.isUnconditional()
                            && java.util.Objects.equals(value.getCondition(), item.getCondition())
                            && java.util.Objects.equals(value.getException(), item.getException())
                            && java.util.Objects.equals(value.getReplacesKnowledgeId(), item.getReplacesKnowledgeId()));
            if (!exists) {
                entry.getConditions().add(KnowledgeCondition.builder().unconditional(item.isUnconditional()).condition(item.getCondition())
                        .exception(item.getException()).replacesKnowledgeId(item.getReplacesKnowledgeId()).build());
            }
        }
    }

    /** 创建精简适用关系。 */
    public RelatedComponent relation(KnowledgeItem row) {
        return RelatedComponent.builder().id(row.getId()).targetType(row.getTargetType()).componentId(row.getTargetId())
                .unconditional(row.isUnconditional()).condition(row.getConditionText()).exception(row.getExceptionText())
                .replacesKnowledgeId(row.getReplacesKnowledgeId()).build();
    }

    private void fillExamples(InterfaceContext context, List<ContractExample> examples, ComponentKnowledgeResponse response) {
        for (ContractExample row : examples) {
            if (!context.getInterfaceId().equals(row.getInterfaceId())) {
                continue;
            }
            try {
                JsonNode request = mapper.reader().with(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                        .readTree(row.getRequestBody());
                JsonNode result = mapper.reader().with(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                        .readTree(row.getResponseBody());
                if (request == null || result == null) {
                    issue(response, "EXAMPLE_JSON_INVALID", row.getId());
                    continue;
                }
                context.getExamples().add(InterfaceExample.builder().id(row.getId()).name(row.getName()).description(row.getDescription())
                        .request(row.getRequestBody()).response(row.getResponseBody()).build());
            } catch (JsonProcessingException exception) {
                issue(response, "EXAMPLE_JSON_INVALID", row.getId());
            }
        }
    }

    private void fillErrors(InterfaceContext context, List<ContractError> errors) {
        for (ContractError row : errors) {
            if (context.getInterfaceId().equals(row.getInterfaceId())) {
                context.getErrors().add(InterfaceError.builder().id(row.getId()).code(row.getCode()).httpStatus(row.getHttpStatus())
                        .description(row.getDescription()).triggerCondition(row.getTriggerCondition()).build());
            }
        }
    }

    private void fillRules(InterfaceContext context, ComponentKnowledgeResponse response) {
        Map<String, KnowledgeItemResponse> entries = Maps.newLinkedHashMap();
        for (KnowledgeItemResponse entry : response.getBusinessKnowledge()) {
            entries.put(entry.getId(), entry);
        }
        for (KnowledgeItemResponse entry : response.getTechnicalKnowledge()) {
            entries.put(entry.getId(), entry);
        }
        for (RelatedComponentGroup group : response.getRelatedComponents()) {
            boolean applies = group.getItems().stream()
                    .anyMatch(item -> "INTERFACE".equals(item.getTargetType()) && context.getInterfaceId().equals(item.getComponentId()));
            if (!applies) {
                continue;
            }
            KnowledgeItemResponse entry = entries.get(group.getKnowledgeId());
            context.getRules().add(InterfaceRule.builder().knowledgeId(entry.getId()).title(entry.getTitle()).type(entry.getType())
                    .kind(entry.getKind()).content(entry.getContent()).status(entry.getStatus()).build());
        }
    }

    private void issue(ComponentKnowledgeResponse response, String type, String target) {
        response.getIssues().add(KnowledgeIssue.builder().type(type).target(target).build());
    }
}