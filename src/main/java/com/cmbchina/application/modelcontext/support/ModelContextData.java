package com.cmbchina.application.modelcontext.support;
import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
import com.cmbchina.domain.knowledge.model.KnowledgeQuery;
import com.cmbchina.domain.knowledge.model.KnowledgeItem;
import com.cmbchina.application.modelcontext.api.dto.ModelIssue;
/** 两个模型用例共享的只读检索结果。 */
@Getter
@Setter
@Builder
public class ModelContextData {
    private KnowledgeQuery query;
    @Builder.Default
    private java.util.List<KnowledgeItem> projects = com.google.common.collect.Lists.newArrayList();
    @Builder.Default
    private java.util.List<KnowledgeItem> services = com.google.common.collect.Lists.newArrayList();
    private KnowledgeItem page;
    private KnowledgeItem component;
    @Builder.Default
    private java.util.List<KnowledgeItem> interfaces = com.google.common.collect.Lists.newArrayList();
    @Builder.Default
    private java.util.List<KnowledgeItem> knowledge = com.google.common.collect.Lists.newArrayList();
    @Builder.Default
    private java.util.List<ModelIssue> issues = com.google.common.collect.Lists.newArrayList();
}
