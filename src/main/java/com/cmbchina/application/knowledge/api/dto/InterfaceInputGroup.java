package com.cmbchina.application.knowledge.api.dto;
import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
/** 按参数位置分组的请求字段。 */
@Getter
@Setter
@Builder
public class InterfaceInputGroup {
    private String location;
    @Builder.Default
    private java.util.List<InterfaceField> fields = com.google.common.collect.Lists.newArrayList();
}
