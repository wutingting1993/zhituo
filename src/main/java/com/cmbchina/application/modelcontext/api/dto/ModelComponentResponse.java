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
public class ModelComponentResponse {
    private String systemCode;
    private String projectCode;
    private ModelPage page;
    private ModelComponent component;
    @Builder.Default
    private java.util.List<ModelKnowledge> businessKnowledge = com.google.common.collect.Lists.newArrayList();
    @Builder.Default
    private java.util.List<ModelKnowledge> technicalKnowledge = com.google.common.collect.Lists.newArrayList();
    @Builder.Default
    private java.util.List<ModelInterface> interfaces = com.google.common.collect.Lists.newArrayList();
    @Builder.Default
    private java.util.List<ModelIssue> issues = com.google.common.collect.Lists.newArrayList();

}
