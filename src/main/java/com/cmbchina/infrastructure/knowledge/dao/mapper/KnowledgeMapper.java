package com.cmbchina.infrastructure.knowledge.dao.mapper;
import com.cmbchina.domain.knowledge.model.KnowledgeQuery;
import com.cmbchina.infrastructure.knowledge.dao.po.KnowledgePo;
import org.apache.ibatis.annotations.Mapper;
/** 知识SQL入口。 */
@Mapper
public interface KnowledgeMapper {
    java.util.List<KnowledgePo> projects(KnowledgeQuery query);

    java.util.List<KnowledgePo> components(KnowledgeQuery query);

    /** 读取页面内登记的组件名称。 */
    java.util.List<KnowledgePo> pageComponents(KnowledgeQuery query);

    java.util.List<KnowledgePo> scopes(KnowledgeQuery query);

    java.util.List<KnowledgePo> services(KnowledgeQuery query);

    java.util.List<KnowledgePo> interfaces(KnowledgeQuery query);

    java.util.List<KnowledgePo> knowledge(KnowledgeQuery query);

    /** 批量读取结构化上下文。 */
    java.util.List<com.cmbchina.infrastructure.knowledge.dao.po.ContractFieldPo> fields(KnowledgeQuery query);

    /** 批量读取结构化上下文。 */
    java.util.List<com.cmbchina.infrastructure.knowledge.dao.po.ContractExamplePo> examples(KnowledgeQuery query);

    /** 批量读取结构化上下文。 */
    java.util.List<com.cmbchina.infrastructure.knowledge.dao.po.ContractErrorPo> errors(KnowledgeQuery query);

    /** 读取当前页面基本信息。 */
    java.util.List<KnowledgePo> pages(KnowledgeQuery query);

    /** 查询同项目中显式关联知识的其他组件。 */
    java.util.List<KnowledgePo> relatedComponents(KnowledgeQuery query);

    /** 按系统项目及明确ID读取模型补充资料。 */
    java.util.List<KnowledgePo> knowledgeByIds(KnowledgeQuery query);

    /** 按系统项目及明确ID读取模型补充资料。 */
    java.util.List<KnowledgePo> interfacesByIds(KnowledgeQuery query);

    /** 读取当前组件的交互，按业务顺序返回。 */
    java.util.List<com.cmbchina.infrastructure.knowledge.dao.po.ComponentInteractionPo> interactions(KnowledgeQuery query);

    /** 读取同项目内当前组件的依赖和影响关系。 */
    java.util.List<com.cmbchina.infrastructure.knowledge.dao.po.ComponentRelationPo> componentRelations(KnowledgeQuery query);
}
