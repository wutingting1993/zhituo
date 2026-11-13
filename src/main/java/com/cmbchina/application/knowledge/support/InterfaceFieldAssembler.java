package com.cmbchina.application.knowledge.support;
import com.cmbchina.application.knowledge.api.dto.InterfaceField;
import com.cmbchina.application.knowledge.api.dto.InterfaceInputGroup;
import com.cmbchina.application.knowledge.api.dto.InterfaceContext;
import com.cmbchina.application.knowledge.api.dto.KnowledgeIssue;
import com.cmbchina.domain.knowledge.model.ContractField;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Objects;
import org.springframework.stereotype.Component;
/** 将合法字段组装为树，隔离无效字段。 */
@Component
public class InterfaceFieldAssembler {
    /** 补齐某个接口字段树。 */
    public void fill(InterfaceContext context, List<ContractField> rows, List<KnowledgeIssue> issues) {
        Map<String, ContractField> records = Maps.newLinkedHashMap();
        for (ContractField row : rows) {
            if (context.getInterfaceId().equals(row.getInterfaceId())) {
                records.put(row.getId(), row);
            }
        }
        Map<String, InterfaceField> nodes = Maps.newLinkedHashMap();
        for (ContractField row : records.values()) {
            if (!valid(row, records)) {
                issues.add(KnowledgeIssue.builder().type("FIELD_STRUCTURE_INVALID").target(row.getId()).build());
                continue;
            }
            nodes.put(row.getId(), convert(row));
        }
        Map<String, InterfaceInputGroup> inputs = Maps.newLinkedHashMap();
        for (ContractField row : records.values()) {
            InterfaceField node = nodes.get(row.getId());
            if (node == null) {
                continue;
            }
            if (row.getParentId() != null) {
                nodes.get(row.getParentId()).getChildren().add(node);
            } else if ("REQUEST".equals(row.getDirection())) {
                InterfaceInputGroup group = inputs.get(row.getLocation());
                if (group == null) {
                    group = InterfaceInputGroup.builder().location(row.getLocation()).build();
                    inputs.put(row.getLocation(), group);
                }
                group.getFields().add(node);
            } else {
                context.getOutputs().add(node);
            }
        }
        context.getInputs().addAll(inputs.values());
    }

    private boolean valid(ContractField row, Map<String, ContractField> records) {
        if (!"REQUEST".equals(row.getDirection()) && !"RESPONSE".equals(row.getDirection())) {
            return false;
        }
        Set<String> visited = Sets.newHashSet();
        ContractField current = row;
        while (current != null) {
            if (!visited.add(current.getId())) {
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

    private InterfaceField convert(ContractField row) {
        return InterfaceField.builder().id(row.getId()).name(row.getFieldName()).path(row.getFieldPath()).location(row.getLocation())
                .type(row.getValueType()).itemType(row.getItemType()).required(row.isRequiredFlag()).nullable(row.isNullableFlag())
                .meaning(row.getBusinessMeaning()).detail(row.getTechnicalDescription()).validation(row.getValidationText()).build();
    }
}