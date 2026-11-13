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
public class InterfaceContext {
    private String method;
    private String address;
    private String serviceCode;
    private String path;
    private String interfaceId;
    private String name;
    private String description;
    private String requestPath;
    private String requestType;
    private String responseType;
    @Builder.Default
    private java.util.List<InterfaceInputGroup> inputs = com.google.common.collect.Lists.newArrayList();
    @Builder.Default
    private java.util.List<InterfaceField> outputs = com.google.common.collect.Lists.newArrayList();
    @Builder.Default
    private java.util.List<InterfaceRule> rules = com.google.common.collect.Lists.newArrayList();
    @Builder.Default
    private java.util.List<InterfaceError> errors = com.google.common.collect.Lists.newArrayList();
    @Builder.Default
    private java.util.List<InterfaceExample> examples = com.google.common.collect.Lists.newArrayList();
}
