package com.cmbchina.application.knowledge.api.dto;
import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
/** 按知识分组的适用关系。 */
@Getter
@Setter
@Builder
public class RelatedComponentGroup {
    private String knowledgeId;
    @Builder.Default
    private java.util.List<RelatedComponent> items = com.google.common.collect.Lists.newArrayList();
}
