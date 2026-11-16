package com.cmbchina.domain.knowledge.support;
import com.cmbchina.domain.knowledge.model.KnowledgeQuery;
import com.cmbchina.domain.knowledge.model.KnowledgeItem;
/** 知识读取边界。 */
public interface KnowledgeDataSupport {
    java.util.List<KnowledgeItem> projects(KnowledgeQuery query);

    java.util.List<KnowledgeItem> components(KnowledgeQuery query);

    /** 读取页面内登记的组件名称。 */
    java.util.List<KnowledgeItem> pageComponents(KnowledgeQuery query);

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

    /** 按系统项目及明确ID读取模型补充资料。 */
    java.util.List<KnowledgeItem> knowledgeByIds(KnowledgeQuery query);

    /** 按系统项目及明确ID读取模型补充资料。 */
    java.util.List<KnowledgeItem> interfacesByIds(KnowledgeQuery query);

    /** 读取当前组件的交互，按业务顺序返回。 */
    java.util.List<com.cmbchina.domain.knowledge.model.ComponentInteraction> interactions(KnowledgeQuery query);

    /** 读取同项目内当前组件的依赖和影响关系。 */
    java.util.List<com.cmbchina.domain.knowledge.model.ComponentRelation> componentRelations(KnowledgeQuery query);
}
