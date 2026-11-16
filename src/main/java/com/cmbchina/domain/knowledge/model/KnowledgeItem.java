package com.cmbchina.domain.knowledge.model;
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
public class KnowledgeItem {
    private String id;
    private String systemCode;
    private String projectCode;
    private String serviceCode;
    private String serviceType;
    private String method;
    private String path;
    private String localPath;
    private KnowledgeKind kind;
    private String content;
    private String source;
    private String status;
    private String knowledgeId;
    private String targetType;
    private String targetId;
    private String conditionText;
    private boolean unconditional;
    private String exceptionText;
    private String replacesKnowledgeId;
    private String replacesStatus;
    private String title;
    private String knowledgeType;
    private String replacesTitle;
    private String replacesType;
    private String componentKey;
    private String name;
    private String description;
    private String requestContentType;
    private String responseContentType;
}
