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
public class KnowledgeQuery {
    private String systemCode;
    private String projectCode;
    private String pagePath;
    private String componentId;
    private String currentComponentId;
    private java.util.List<String> knowledgeIds;
    private java.util.List<String> targetKeys;
    private java.util.List<String> interfaceIds;
}
