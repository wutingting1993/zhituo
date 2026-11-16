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
public class ModelField {
    private String name;
    private String path;
    private String location;
    private String validation;
    private String businessRule;
    private String technicalRule;
    private boolean required;
    private boolean nullable;
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY)
    @Builder.Default
    private java.util.List<ModelField> children = com.google.common.collect.Lists.newArrayList();

}
