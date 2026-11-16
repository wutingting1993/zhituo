package com.cmbchina.application.knowledge.api.dto;
import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
/** 同一关联组件的全部联动关系。 */
@Getter
@Setter
@Builder
public class ComponentRelationGroup {
    private LinkedComponentResponse component;
    @Builder.Default
    private java.util.List<ComponentRelationResponse> items = com.google.common.collect.Lists.newArrayList();
}
