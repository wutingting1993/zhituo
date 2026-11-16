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
public class ModelInterface {
    private String id;
    private String name;
    private String description;
    private String serviceCode;
    private String method;
    private String path;
    private java.util.List<ModelInputGroup> inputs;
    private java.util.List<ModelField> outputs;
    private java.util.List<ModelRule> rules;
    private java.util.List<ModelError> errors;
    private java.util.List<ModelExample> examples;

}
