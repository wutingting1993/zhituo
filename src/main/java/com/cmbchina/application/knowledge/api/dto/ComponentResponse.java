package com.cmbchina.application.knowledge.api.dto;
import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
/** 页面组件基本信息。 */
@Getter
@Setter
@Builder
public class ComponentResponse {
    private String id;
    private String componentId;
    private String name;
    private String description;
}
