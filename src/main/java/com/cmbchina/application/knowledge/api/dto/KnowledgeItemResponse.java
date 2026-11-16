package com.cmbchina.application.knowledge.api.dto;
import com.cmbchina.domain.knowledge.model.KnowledgeKind;
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
public class KnowledgeItemResponse {
    private String id;
    private String title;
    private String type;
    private KnowledgeKind kind;
    private String content;
    private String status;
    @Builder.Default
    private java.util.List<KnowledgeCondition> conditions = com.google.common.collect.Lists.newArrayList();
}
