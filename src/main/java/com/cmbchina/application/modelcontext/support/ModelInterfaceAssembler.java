package com.cmbchina.application.modelcontext.support;
import com.cmbchina.application.modelcontext.api.dto.ModelInterface;
import com.cmbchina.application.modelcontext.api.dto.ModelRule;
import com.cmbchina.application.modelcontext.api.dto.ModelKnowledge;
import com.cmbchina.application.modelcontext.api.dto.ModelIssue;
import com.cmbchina.application.modelcontext.api.dto.ModelError;
import com.cmbchina.application.modelcontext.api.dto.ModelExample;
import com.cmbchina.domain.knowledge.model.KnowledgeItem;
import com.cmbchina.domain.knowledge.model.ContractField;
import com.cmbchina.domain.knowledge.model.ContractExample;
import com.cmbchina.domain.knowledge.model.ContractError;
import com.cmbchina.domain.knowledge.support.KnowledgeDataSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
/** 按需提供接口字段及规则，正文全局只定义一次。 */
@Component
public class ModelInterfaceAssembler {
    @Autowired
    private KnowledgeDataSupport support;
    @Autowired
    private ModelFieldAssembler fields;
    @Autowired
    private ObjectMapper mapper;
    /** 批量读取被请求部分。 */
    public List<ModelInterface> assemble(ModelContextData data, Map<String, Set<String>> requested, Map<String, ModelKnowledge> knowledge,
            Set<String> defined, boolean first) {
        List<ContractField> rows = fieldRows(data, requested);
        List<ContractExample> examples = has(requested, "examples") ? support.examples(data.getQuery()) : Lists.newArrayList();
        List<ContractError> errors = has(requested, "errors") ? support.errors(data.getQuery()) : Lists.newArrayList();
        List<ModelInterface> result = Lists.newArrayList();
        for (KnowledgeItem row : data.getInterfaces()) {
            ModelInterface target = ModelInterface.builder().id(row.getId()).name(row.getName()).description(row.getDescription())
                    .serviceCode(row.getServiceCode()).method(row.getMethod()).path(row.getPath()).build();
            Set<String> parts = requested.get(row.getId());
            if (parts == null) {
                parts = first ? Sets.newHashSet("inputs", "outputs", "rules") : Sets.newHashSet();
            }
            if (row.getId() == null) {
                target.setInputs(Lists.newArrayList());
                target.setOutputs(Lists.newArrayList());
                target.setRules(Lists.newArrayList());
                result.add(target);
                continue;
            }
            fill(target, parts, rows, data);
            if (parts.contains("rules")) {
                rules(target, data, knowledge, defined, first);
            }
            if (parts.contains("examples")) {
                examples(target, examples, data.getIssues());
            }
            if (parts.contains("errors")) {
                errors(target, errors);
            }
            result.add(target);
        }
        return result;
    }

    private List<ContractField> fieldRows(ModelContextData data, Map<String, Set<String>> requested) {
        if (has(requested, "inputs") || has(requested, "outputs")) {
            return support.fields(data.getQuery());
        }
        return Lists.newArrayList();
    }

    private void fill(ModelInterface target, Set<String> parts, List<ContractField> rows, ModelContextData data) {
        if (!parts.contains("inputs") && !parts.contains("outputs")) {
            return;
        }
        fields.fill(target, rows, parts, data.getIssues());
        if (target.getInputs() != null && target.getOutputs() != null && target.getInputs().isEmpty() && target.getOutputs().isEmpty()) {
            data.getIssues().add(ModelIssue.builder().type("INTERFACE_CONTRACT_MISSING").target(target.getId()).build());
        }
    }

    private boolean has(Map<String, Set<String>> requested, String part) {
        return requested.values().stream().anyMatch(p -> p.contains(part));
    }

    private void rules(ModelInterface target, ModelContextData data, Map<String, ModelKnowledge> knowledge, Set<String> defined, boolean first) {
        target.setRules(Lists.newArrayList());
        Set<String> included = Sets.newHashSet();
        for (KnowledgeItem link : data.getKnowledge()) {
            if (!"INTERFACE".equals(link.getTargetType()) || !target.getId().equals(link.getTargetId())
                    || !knowledge.containsKey(link.getKnowledgeId()) || !included.add(link.getKnowledgeId())) {
                continue;
            }
            ModelKnowledge entry = knowledge.get(link.getKnowledgeId());
            ModelRule rule = ModelRule.builder().knowledgeId(entry.getId()).build();
            if (first && defined.add(entry.getId())) {
                rule.setContent(entry.getContent());
                rule.setStatus(entry.getStatus());
                rule.setConditions(entry.getConditions());
            }
            target.getRules().add(rule);
        }
    }

    private void examples(ModelInterface target, List<ContractExample> rows, List<ModelIssue> issues) {
        target.setExamples(Lists.newArrayList());
        ObjectReader reader = mapper.reader().with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
        for (ContractExample row : rows) {
            if (!target.getId().equals(row.getInterfaceId())) {
                continue;
            }
            try {
                if (reader.readTree(row.getRequestBody()) == null || reader.readTree(row.getResponseBody()) == null) {
                    invalidExample(issues, row.getId());
                    continue;
                }
                target.getExamples().add(ModelExample.builder().id(row.getId()).name(row.getName()).description(row.getDescription())
                        .request(row.getRequestBody()).response(row.getResponseBody()).build());
            } catch (JsonProcessingException exception) {
                invalidExample(issues, row.getId());
            }
        }
    }

    private void invalidExample(List<ModelIssue> issues, String id) {
        issues.add(ModelIssue.builder().type("EXAMPLE_JSON_INVALID").target(id).build());
    }

    private void errors(ModelInterface target, List<ContractError> rows) {
        target.setErrors(Lists.newArrayList());
        for (ContractError row : rows) {
            if (target.getId().equals(row.getInterfaceId())) {
                target.getErrors().add(ModelError.builder().code(row.getCode()).httpStatus(row.getHttpStatus()).description(row.getDescription())
                        .triggerCondition(row.getTriggerCondition()).build());
            }
        }
    }
}