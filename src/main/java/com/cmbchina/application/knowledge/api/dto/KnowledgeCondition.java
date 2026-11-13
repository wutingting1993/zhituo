package com.cmbchina.application.knowledge.api.dto;
import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
/** 当前查询上下文中的适用条件。 */
@Getter
@Setter
@Builder
public class KnowledgeCondition {
    private boolean unconditional;
    private String condition;
    private String exception;
    private String replacesKnowledgeId;
}
