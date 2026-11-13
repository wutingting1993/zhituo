package com.cmbchina.application.knowledge.api.dto;
import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
/** 知识适用关系，仅返回定位和条件。 */
@Getter
@Setter
@Builder
public class RelatedComponent {
    private String id;
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String targetType;
    private String componentId;
    private boolean unconditional;
    private String condition;
    private String exception;
    private String replacesKnowledgeId;
}
