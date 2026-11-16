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
public class ModelInterfaceRequest {
    @javax.validation.constraints.NotBlank(message = "接口方法不能为空")
    private String method;
    @javax.validation.constraints.NotBlank(message = "接口地址不能为空")
    private String address;
}
