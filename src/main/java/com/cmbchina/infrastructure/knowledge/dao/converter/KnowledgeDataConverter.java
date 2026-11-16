package com.cmbchina.infrastructure.knowledge.dao.converter;
import com.cmbchina.domain.knowledge.model.KnowledgeItem;
import com.cmbchina.infrastructure.knowledge.dao.po.KnowledgePo;
/** 转换持久化知识。 */
public final class KnowledgeDataConverter {
    private KnowledgeDataConverter() {
    }

    /** 转换一条查询记录。 */
    public static KnowledgeItem convert(KnowledgePo po) {
        return KnowledgeItem.builder().id(po.getId()).systemCode(po.getSystemCode()).projectCode(po.getProjectCode()).serviceCode(po.getServiceCode())
                .serviceType(po.getServiceType()).method(po.getMethod()).path(po.getPath()).localPath(po.getLocalPath()).kind(po.getKind())
                .content(po.getContent()).source(po.getSource()).status(po.getStatus()).knowledgeId(po.getKnowledgeId())
                .targetType(po.getTargetType()).targetId(po.getTargetId()).conditionText(po.getConditionText()).unconditional(po.isUnconditional())
                .exceptionText(po.getExceptionText()).replacesKnowledgeId(po.getReplacesKnowledgeId()).replacesStatus(po.getReplacesStatus())
                .title(po.getTitle()).knowledgeType(po.getKnowledgeType()).replacesTitle(po.getReplacesTitle()).replacesType(po.getReplacesType())
                .componentKey(po.getComponentKey()).name(po.getName()).description(po.getDescription()).requestContentType(po.getRequestContentType())
                .responseContentType(po.getResponseContentType()).build();
    }

    /** 转换结构化记录。 */
    public static com.cmbchina.domain.knowledge.model.ContractField convert(com.cmbchina.infrastructure.knowledge.dao.po.ContractFieldPo po) {
        return com.cmbchina.domain.knowledge.model.ContractField.builder().id(po.getId()).interfaceId(po.getInterfaceId())
                .direction(po.getDirection()).location(po.getLocation()).parentId(po.getParentId()).fieldName(po.getFieldName())
                .fieldPath(po.getFieldPath()).valueType(po.getValueType()).itemType(po.getItemType()).businessMeaning(po.getBusinessMeaning())
                .technicalDescription(po.getTechnicalDescription()).validationText(po.getValidationText()).requiredFlag(po.isRequiredFlag())
                .nullableFlag(po.isNullableFlag()).build();
    }

    /** 转换结构化记录。 */
    public static com.cmbchina.domain.knowledge.model.ContractExample convert(com.cmbchina.infrastructure.knowledge.dao.po.ContractExamplePo po) {
        return com.cmbchina.domain.knowledge.model.ContractExample.builder().id(po.getId()).interfaceId(po.getInterfaceId()).name(po.getName())
                .description(po.getDescription()).requestBody(po.getRequestBody()).responseBody(po.getResponseBody()).build();
    }

    /** 转换结构化记录。 */
    public static com.cmbchina.domain.knowledge.model.ContractError convert(com.cmbchina.infrastructure.knowledge.dao.po.ContractErrorPo po) {
        return com.cmbchina.domain.knowledge.model.ContractError.builder().id(po.getId()).interfaceId(po.getInterfaceId()).code(po.getCode())
                .description(po.getDescription()).triggerCondition(po.getTriggerCondition()).httpStatus(po.getHttpStatus()).build();
    }

    /** 转换组件交互或联动记录。 */
    public static com.cmbchina.domain.knowledge.model.ComponentInteraction convert(
            com.cmbchina.infrastructure.knowledge.dao.po.ComponentInteractionPo po) {
        return com.cmbchina.domain.knowledge.model.ComponentInteraction.builder().id(po.getId()).componentId(po.getComponentId())
                .action(po.getAction()).triggerCondition(po.getTriggerCondition()).providedContent(po.getProvidedContent()).response(po.getResponse())
                .build();
    }

    /** 转换组件交互或联动记录。 */
    public static com.cmbchina.domain.knowledge.model.ComponentRelation convert(com.cmbchina.infrastructure.knowledge.dao.po.ComponentRelationPo po) {
        return com.cmbchina.domain.knowledge.model.ComponentRelation.builder().id(po.getId()).interactionId(po.getInteractionId())
                .action(po.getAction()).triggerCondition(po.getTriggerCondition()).providedContent(po.getProvidedContent()).response(po.getResponse())
                .sourceId(po.getSourceId()).sourceComponentId(po.getSourceComponentId()).sourceName(po.getSourceName())
                .sourcePagePath(po.getSourcePagePath()).targetId(po.getTargetId()).targetComponentId(po.getTargetComponentId())
                .targetName(po.getTargetName()).targetPagePath(po.getTargetPagePath()).effect(po.getEffect()).condition(po.getCondition()).build();
    }

}
