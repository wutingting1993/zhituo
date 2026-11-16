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
public class ModelComponent {
    private String id;
    private String componentId;
    private String name;
    private String description;
}
