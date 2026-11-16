package com.cmbchina.application.modelcontext.support;
import com.cmbchina.application.modelcontext.api.dto.ModelInterface;
import com.cmbchina.application.modelcontext.api.dto.ModelField;
import com.cmbchina.application.modelcontext.api.dto.ModelInputGroup;
import com.cmbchina.application.modelcontext.api.dto.ModelIssue;
import com.cmbchina.domain.knowledge.model.ContractField;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Objects;
import org.springframework.stereotype.Component;
/** 投影字段口径，不摘要或拼接原文。 */
@Component
public class ModelFieldAssembler {
    /** 只组装被请求的方向。 */
    public void fill(ModelInterface target, List<ContractField> rows, Set<String> parts, List<ModelIssue> issues) {
        Map<String, ContractField> records = Maps.newLinkedHashMap();
        for (ContractField row : rows) {
            if (target.getId().equals(row.getInterfaceId()) && requested(row, parts)) {
                records.put(row.getId(), row);
            }
        }
        Map<String, ModelField> nodes = Maps.newLinkedHashMap();
        for (ContractField row : records.values()) {
            if (!valid(row, records)) {
                issues.add(ModelIssue.builder().type("FIELD_STRUCTURE_INVALID").target(row.getId()).build());
                continue;
            }
            nodes.put(row.getId(), convert(row));
        }
        if (parts.contains("inputs")) {
            target.setInputs(Lists.newArrayList());
        }
        if (parts.contains("outputs")) {
            target.setOutputs(Lists.newArrayList());
        }
        Map<String, ModelInputGroup> inputs = Maps.newLinkedHashMap();
        for (ContractField row : records.values()) {
            attach(row, nodes, parts, inputs, target);
        }
        if (target.getInputs() != null) {
            target.getInputs().addAll(inputs.values());
        }
    }

    private boolean requested(ContractField row, Set<String> parts) {
        if ("REQUEST".equals(row.getDirection())) {
            return parts.contains("inputs");
        }
        if ("RESPONSE".equals(row.getDirection())) {
            return parts.contains("outputs");
        }
        return parts.contains("inputs") || parts.contains("outputs");
    }

    private void attach(ContractField row, Map<String, ModelField> nodes, Set<String> parts, Map<String, ModelInputGroup> inputs,
            ModelInterface target) {
        ModelField node = nodes.get(row.getId());
        if (node == null) {
            return;
        }
        if (row.getParentId() != null) {
            nodes.get(row.getParentId()).getChildren().add(node);
            return;
        }
        if ("REQUEST".equals(row.getDirection()) && parts.contains("inputs")) {
            input(row, node, inputs);
        } else if ("RESPONSE".equals(row.getDirection()) && parts.contains("outputs")) {
            target.getOutputs().add(node);
        }
    }

    private void input(ContractField row, ModelField node, Map<String, ModelInputGroup> inputs) {
        ModelInputGroup group = inputs.get(row.getLocation());
        if (group == null) {
            group = ModelInputGroup.builder().location(row.getLocation()).build();
            inputs.put(row.getLocation(), group);
        }
        group.getFields().add(node);
    }

    private boolean valid(ContractField row, Map<String, ContractField> records) {
        if (!"REQUEST".equals(row.getDirection()) && !"RESPONSE".equals(row.getDirection())) {
            return false;
        }
        Set<String> seen = Sets.newHashSet();
        ContractField current = row;
        while (current != null) {
            if (!seen.add(current.getId())) {
                return false;
            }
            if (current.getParentId() == null) {
                return true;
            }
            ContractField parent = records.get(current.getParentId());
            if (!compatible(row, parent)) {
                return false;
            }
            current = parent;
        }
        return false;
    }

    private boolean compatible(ContractField row, ContractField parent) {
        return parent != null && Objects.equals(row.getDirection(), parent.getDirection()) && Objects.equals(row.getLocation(), parent.getLocation())
                && ("object".equals(parent.getValueType()) || "array<object>".equals(parent.getValueType()));
    }

    private ModelField convert(ContractField row) {
        return ModelField.builder().name(row.getFieldName()).path(row.getFieldPath())
                .location("RESPONSE".equals(row.getDirection()) ? row.getLocation() : null).required(row.isRequiredFlag())
                .nullable(row.isNullableFlag()).validation(row.getValidationText()).businessRule(row.getBusinessMeaning())
                .technicalRule(row.getTechnicalDescription()).build();
    }
}