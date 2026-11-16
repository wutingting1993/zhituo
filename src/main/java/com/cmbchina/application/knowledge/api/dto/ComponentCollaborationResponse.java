package com.cmbchina.application.knowledge.api.dto;
import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
/** 当前组件的依赖来源和影响范围。 */
@Getter
@Setter
@Builder
public class ComponentCollaborationResponse {
    @Builder.Default
    private java.util.List<ComponentRelationGroup> inputs = com.google.common.collect.Lists.newArrayList();
    @Builder.Default
    private java.util.List<ComponentRelationGroup> outputs = com.google.common.collect.Lists.newArrayList();
}
