# 模型组件上下文 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:executing-plans；用户已明确实施，沿用当前会话直接执行。

**Goal:** 新增独立模型首包和按需详情接口，保持前端展示接口不变。
**Architecture:** adapter.modelcontext → application.modelcontext.api/service/support → 现有domain.knowledge.support。独立DTO投影及字段树，不调用或修改前端DTO。补充按ID、系统项目范围读取知识和接口的MyBatis查询。
**Tech Stack:** Java8、Spring Boot2.7.2、MyBatis XML、MySQL，复用依赖。
**Spec:** ../specs/2026-11-13-model-context-design.md（用户已要求实施）。

## Global Constraints

不改前端接口契约；无DDL/数据迁移；不返回source、类型或首包样例；字段businessRule/technicalRule直接取business_meaning/technical_description，不摘要。不自动新增运行单元/e2e测试，使用必要静态、编译、架构检查及已有真实数据HTTP验证。不自动提交Git。

## Review Focus

字段映射逐值一致；跨项目ID和部分缺失；必要口径无悬空引用及重复正文；详情仅返回请求parts；前端真实响应保持一致。

## Task 1：独立契约及范围查询

- [x] 新增application/modelcontext/api/dto下请求、模型字段/知识/规则/接口及顶层响应类型。两个API仅使用独立DTO。
- [x] 扩展domain.knowledge.support.KnowledgeDataSupport和KnowledgeMapper两个按ID查询接口：knowledgeByIds(KnowledgeQuery)、interfacesByIds(KnowledgeQuery)；XML限定systemCode/projectCode和ID列表，空列表不全量查询；基础设施PO及转换复用。
- [x] 请求字段及parts/include校验；空详情请求明确失败，不引入session/contextId。

## Task 2：首包与按需组装

- [x] ModelContextLoader.load(scope,interfaces)读取页面、组件、作用范围及接口，直接使用领域数据；ModelInterfaceResolver处理服务前缀、方法、模板和歧义，按服务方法路径去重。
- [x] ModelKnowledgeAssembler.collect(rows,issues)按ID聚合正文/conditions；状态、待确认及缺少条件明确返回。首包业务完整、技术仅目录；公共规则按ID全文一次，其余引用，条件随正文保留。
- [x] ModelFieldAssembler.fill(interface,rows,parts,issues)构建模型字段树；输入按location分组，输出带必要位置。无type/样例/字段ID，两个Rule字段逐值取领域对象对应原列。孤儿和环按项提示。
- [x] ModelContextApplicationService.component(ModelComponentRequest):ModelComponentResponse；details(ModelDetailsRequest):ModelDetailsResponse实现批量parts、缺失和项目隔离。补充知识的当前条件未知时CONTEXT_SCOPE_UNKNOWN。relatedComponents按同项目其他显式关联；codeLocations只当前项目/命中服务。

## Task 3：入口与文档

- [x] 新增adapter/modelcontext/web/ModelContextController.java：POST /api/v1/model-context/component和/details。
- [x] 复用现有上下文专用NON_NULL序列化，扩展严格路由集合；前端响应前后逐值比较。
- [x] 新增docs/model-context-api.md及真实请求响应记录，列出输入输出、校验、错误、口径、投影及缺失边界。

## Task 4：必要验证

- [x] 对本次Java范围执行scripts/java-quality.sh verify，运行scripts/backend-gate.sh src/main/java/，修复后重跑。
- [x] 真实HTTP首包验证13条业务、11技术目录、6输入25输出，直接映射字段，技术规则全文去重且引用可解析；空/未知接口等部分结果。
- [x] 详情验证parts合并、重复ID、跨项目/缺失ID、未知条件范围、错误及String样例按需、代码和其他组件仅明确请求返回。
- [x] 前端响应与原记录一致，计算紧凑JSON大小；字段及文档JSON完整一致。结束临时服务；记录未验证token/生产性能范围。

## 取舍与进度

实施路径与DTO命名按职责拆分，内部范围状态在两个用例间复用，避免依赖前端响应。当前分支codex/component-knowledge已有用户其他报表改动，不额外创建工作树或修改其文件。
