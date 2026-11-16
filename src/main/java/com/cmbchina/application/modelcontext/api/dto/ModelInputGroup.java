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
public class ModelInputGroup {
    private String location;
    @Builder.Default
    private java.util.List<ModelField> fields = com.google.common.collect.Lists.newArrayList();

}
