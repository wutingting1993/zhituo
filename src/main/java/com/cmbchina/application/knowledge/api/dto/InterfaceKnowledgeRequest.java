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
public class InterfaceKnowledgeRequest {
    @javax.validation.constraints.NotBlank(message = "接口方法不能为空")
    private String method;
    @javax.validation.constraints.NotBlank(message = "接口地址不能为空")
    private String address;
}
