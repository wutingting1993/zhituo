package com.cmbchina.application.modelcontext.api.dto;
import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
/** 面向模型的上下文数据。 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelDetailsResponse {
    private java.util.List<ModelKnowledge> knowledge;
    private java.util.List<ModelInterface> interfaces;
    private java.util.List<ModelRelatedGroup> relatedComponents;
    private java.util.List<ModelCodeLocation> codeLocations;
    @Builder.Default
    private java.util.List<ModelIssue> issues = com.google.common.collect.Lists.newArrayList();

}
