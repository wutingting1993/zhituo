package com.cmbchina.domain.knowledge.support;
import com.cmbchina.domain.knowledge.model.KnowledgeQuery;
import com.cmbchina.domain.knowledge.model.KnowledgeItem;
/** 知识读取边界。 */
public interface KnowledgeDataSupport {
    java.util.List<KnowledgeItem> projects(KnowledgeQuery query);

    java.util.List<KnowledgeItem> components(KnowledgeQuery query);

    java.util.List<KnowledgeItem> scopes(KnowledgeQuery query);

    java.util.List<KnowledgeItem> services(KnowledgeQuery query);

    java.util.List<KnowledgeItem> interfaces(KnowledgeQuery query);

    java.util.List<KnowledgeItem> knowledge(KnowledgeQuery query);

    /** 批量读取结构化上下文。 */
    java.util.List<com.cmbchina.domain.knowledge.model.ContractField> fields(KnowledgeQuery query);

    /** 批量读取结构化上下文。 */
    java.util.List<com.cmbchina.domain.knowledge.model.ContractExample> examples(KnowledgeQuery query);

    /** 批量读取结构化上下文。 */
    java.util.List<com.cmbchina.domain.knowledge.model.ContractError> errors(KnowledgeQuery query);

    /** 读取当前页面基本信息。 */
    java.util.List<KnowledgeItem> pages(KnowledgeQuery query);

    /** 查询同项目中显式关联知识的其他组件。 */
    java.util.List<KnowledgeItem> relatedComponents(KnowledgeQuery query);
}
