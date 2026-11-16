package com.cmbchina.application.knowledge.api.dto;
import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/** 知识查询数据。 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComponentKnowledgeResponse {
    @Builder.Default
    private java.util.List<ComponentInteractionResponse> interactions = com.google.common.collect.Lists.newArrayList();
    @Builder.Default
    private ComponentCollaborationResponse collaboration = ComponentCollaborationResponse.builder().build();
    private java.util.List<RelatedComponentGroup> relatedComponents;
    private PageResponse page;
    private ComponentResponse component;
    private java.util.List<KnowledgeItemResponse> businessKnowledge;
    private java.util.List<KnowledgeItemResponse> technicalKnowledge;
    private java.util.List<InterfaceContext> interfaceContexts;
    private java.util.List<CodeLocationResponse> codeLocations;
    private java.util.List<KnowledgeIssue> issues;
}
