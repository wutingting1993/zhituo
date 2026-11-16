package com.cmbchina.application.knowledge.api.dto;
import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
/** 页面组件标识与登记名称。 */
@Getter
@Setter
@Builder
public class ComponentNameResponse {
    private String componentId;
    private String name;
}
