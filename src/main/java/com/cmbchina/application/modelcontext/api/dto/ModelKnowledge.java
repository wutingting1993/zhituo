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
public class ModelKnowledge {
    private String id;
    private String title;
    private String content;
    private String status;
    private java.util.List<ModelCondition> conditions;

}
