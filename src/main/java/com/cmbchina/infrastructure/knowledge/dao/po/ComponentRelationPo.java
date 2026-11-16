package com.cmbchina.infrastructure.knowledge.dao.po;
import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
/** 同项目组件之间的有向联动。 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComponentRelationPo {
    private String id;
    private String interactionId;
    private String action;
    private String triggerCondition;
    private String providedContent;
    private String response;
    private String sourceId;
    private String sourceComponentId;
    private String sourceName;
    private String sourcePagePath;
    private String targetId;
    private String targetComponentId;
    private String targetName;
    private String targetPagePath;
    private String effect;
    private String condition;
}
