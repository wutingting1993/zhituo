package com.cmbchina.application.knowledge.api.dto;
import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
/** 结构化知识数据。 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterfaceField {
    private String id;
    private String name;
    private String path;
    private String location;
    private String type;
    private String itemType;
    private String meaning;
    private String detail;
    private String validation;
    private boolean required;
    private boolean nullable;
    @Builder.Default
    private java.util.List<InterfaceField> children = com.google.common.collect.Lists.newArrayList();
}
