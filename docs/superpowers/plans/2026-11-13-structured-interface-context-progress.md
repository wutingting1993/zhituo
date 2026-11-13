# 实施记录：2026-11-13-structured-interface-context.md

任务1—4已完成，细节及实际命令见../../knowledge/validation/report-summary/README.md。

Ruling：知识分类列为未分类旧数据保留NULL并提示，不设置默认技术分类；避免错误归类，代价为调用方需处理未知分类。新案例全部明确分类。

Ruling：迁移前混合知识使用MIXED_LEGACY保留历史，查询与API_CONTRACT_LEGACY一样排除；代价为历史类不作为现行规则返回，来源仍可追溯。

Ruling：损坏知识验证使用同一Spring事务内直接查询后回滚，HTTP正常场景独立验证；未提交数据无法由另一个HTTP事务看见，不以假数据冒充生产响应。

审阅发现的缺少分类提示已修正；34个Java verify、全量backend-gate通过。5项真实HTTP、事务坏数据隔离及回滚、10条SQL EXPLAIN通过。DDL及迁移重放退出0。未增加编辑接口、单元测试或依赖，未修改原统计业务代码，未提交Git。

最新用户指示覆盖上述历史保留取舍：不兼容旧格式，删除重复定位字段；knowledgeEntries拆为businessKnowledge、technicalKnowledge。用户明确不保留本案例旧数据，定向删除已审核通过。35文件verify及gate通过；五项真实查询断言无兼容字段、当前有效24条知识45条关系保持完整。
