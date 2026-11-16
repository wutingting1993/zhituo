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
public class ModelComponentRequest {
    @javax.validation.constraints.NotBlank(message = "系统编号不能为空")
    private String systemCode;
    @javax.validation.constraints.NotBlank(message = "项目号不能为空")
    private String projectCode;
    @javax.validation.constraints.NotBlank(message = "页面路径不能为空")
    private String pagePath;
    @javax.validation.constraints.NotBlank(message = "组件ID不能为空")
    private String componentId;
    @javax.validation.Valid
    private java.util.List<@javax.validation.constraints.NotNull ModelInterfaceRequest> interfaces;

}
