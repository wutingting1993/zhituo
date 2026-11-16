package com.cmbchina.infrastructure.knowledge.dao.po;
import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
/** 组件内部交互及传递内容。 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComponentInteractionPo {
    private String id;
    private String componentId;
    private String action;
    private String triggerCondition;
    private String providedContent;
    private String response;
}
