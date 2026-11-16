package com.cmbchina.application.knowledge.api.dto;
import com.cmbchina.domain.knowledge.model.KnowledgeKind;
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
public class InterfaceRule {
    private String knowledgeId;
    private String title;
    private String type;
    private KnowledgeKind kind;
    private String content;
    private String status;
}
