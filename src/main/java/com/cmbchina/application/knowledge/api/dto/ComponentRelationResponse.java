package com.cmbchina.application.knowledge.api.dto;
import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
/** 联动操作及来源和目标，两个方向引用相同关系主键。 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComponentRelationResponse {
    private String id;
    private String interactionId;
    private LinkedComponentResponse source;
    private LinkedComponentResponse target;
    private String action;
    private String triggerCondition;
    private String providedContent;
    private String response;
    private String effect;
    private String condition;
}
