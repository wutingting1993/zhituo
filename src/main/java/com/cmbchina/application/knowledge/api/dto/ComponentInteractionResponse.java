package com.cmbchina.application.knowledge.api.dto;
import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
/** 组件交互说明。 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComponentInteractionResponse {
    private String id;
    private String action;
    private String triggerCondition;
    private String providedContent;
    private String response;
}
