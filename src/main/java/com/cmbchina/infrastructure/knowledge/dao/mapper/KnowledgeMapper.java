package com.cmbchina.infrastructure.knowledge.dao.mapper;
import com.cmbchina.domain.knowledge.model.KnowledgeQuery;
import com.cmbchina.infrastructure.knowledge.dao.po.KnowledgePo;
import org.apache.ibatis.annotations.Mapper;
/** 知识SQL入口。 */
@Mapper
public interface KnowledgeMapper {
    java.util.List<KnowledgePo> projects(KnowledgeQuery query);

    java.util.List<KnowledgePo> components(KnowledgeQuery query);

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
}
