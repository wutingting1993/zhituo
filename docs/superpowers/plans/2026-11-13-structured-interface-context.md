# 结构化接口上下文 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans，沿用当前会话直接实施，按任务核对。

**Goal:** interfaceContexts返回结构化契约，可读适用关系区分业务及技术知识。
**Architecture:** 复用knowledge模块和现有知识规则表，增字段、示例、错误三表；领域对象、PO、DTO分别转换。应用辅助组件组装字段树、示例和关系。
**Tech Stack:** 当前Java8、Spring Boot2.7.2、MyBatis XML、MySQL，无新增依赖。
**Spec:** ../specs/2026-11-13-structured-interface-context-design.md（用户2026-11-13批准，字段命名修正一并生效）。

## Global Constraints

只读接口，不新增修改入口；按既有授权执行本模块DDL、真实数据迁移和HTTP验证。不新增单元/e2e测试。保留历史条目，混合知识拆分；新ID通过CUuid生成。数据库禁止JSON类型，示例text解析为JSON值。triggerCondition保留。

## Review Focus

嵌套响应字段层级；字段孤儿及环；损坏JSON示例；未匹配接口仍可部分返回；规则分类与对象展示信息准确。

## Task 1：结构化契约存储及DTO

- [x] 新增database/structured-interface-context.sql及数据迁移SQL。三表字段与已批准设计一致；现有entry增加title、knowledge_type；page/component增加name；interface增加name、description及媒体类型。DDL重放先核对information_schema，不覆盖已存在列。
- [x] 新增domain/knowledge/model和infrastructure/knowledge/dao/po的字段、示例、错误、目标对象；更新KnowledgeQuery.interfaceIds及KnowledgeItem分类标题。每个对象独立PO及转换，禁止DTO/PO混用。
- [x] 新增application/knowledge/api/dto的字段、示例、错误、知识引用、目标、关系、接口规则。沿用简化JSON字段名；字段meaning/detail保留两类说明；所有列表默认空。

## Task 2：读取及组装

- [x] 修改KnowledgeDataSupport、KnowledgeMapper、KnowledgeDataSupportImpl、KnowledgeDataConverter、KnowledgeMapper.xml，新增fields/examples/errors/targets批量读取，按已命中接口及系统项目过滤。
- [x] 新增application/knowledge/support/KnowledgeContextAssembler，fill(query,response)；读取target元数据，补齐专用ApplicabilityResponse；关联规则按INTERFACE targetId分组。
- [x] 构建字段树：检查父字段同接口方向位置；检测孤儿与环，返回FIELD_STRUCTURE_INVALID且其他合法字段保留。示例仅解析合法JSON，失败返回EXAMPLE_JSON_INVALID。
- [x] 修改应用查询：先匹配接口身份，填充名称与媒体类型，query.interfaceIds由命中结果生成；收集业务技术知识及关系后调用assembler。已迁移历史kind使用明确LEGACY标记不作为现行规则，不生成无意义失效提示；正常失效仍提示。

## Task 3：真实数据迁移及契约文档

- [x] 生成6个请求字段、响应全部字段和mostUsedSkill子字段、真实JSON示例、10000和99999错误约定；各对象及字段稳定ID由CUuid生成。
- [x] 为现有知识补title和type，混合正文原ID保留历史，新增业务、技术条目并复制相同适用范围。旧API_CONTRACT转历史；接口规则从现有适用关系获取。
- [x] 同步docs/component-knowledge-api.md、新案例文档及原始JSON，说明破坏性DTO调整和兼容保留定位字段。新增验证记录。

## Task 4：验证

- [x] 对所有新增修改knowledge Java目录运行bash scripts/java-quality.sh verify；全项目bash scripts/backend-gate.sh src/main/java/。均退出0后方可声明代码完成。
- [x] 启动临时服务，真实查询确认inputs/outputs树、meaning/detail、type分类、rules、错误triggerCondition、JSON示例、目标名称路径；覆盖无接口、错误方法、组件不存在和单项接口缺失。
- [x] 在事务内插入后回滚临时坏字段、坏示例，验证局部缺口及合法结果保留；不污染正式数据。检查真实SQL EXPLAIN、全部文档引用与JSON一致性；关闭临时服务。
