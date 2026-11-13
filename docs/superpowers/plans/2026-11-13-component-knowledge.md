# 组件知识查询接入 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在当前服务新增组件知识查询，不改变报表行为。

**Architecture:** 新增 knowledge 模块，Controller → 应用服务接口 → 领域数据支持接口 → MyBatis XML。复用现有 Result、异常处理与数据库连接；知识存 MySQL，仅提供查询。

**Tech Stack:** Java 8、Spring Boot 2.7.2、MyBatis 2.3.2、Maven、MySQL、Lombok、Guava，沿用当前版本。

**Spec:** ../specs/2026-11-13-component-knowledge-design.md；用户最新指示覆盖独立部署边界，改为接入当前服务。

## Global Constraints

- 不考虑录入方式、代码版本匹配；不新增录入接口、前端页面、权限、监控或部署编排。
- 不存组件动作与接口关系，不绑定类方法；条件交给模型判断。
- 保持业务知识正文唯一，保留多条适用关系。
- 生成 SQL 文件，并按用户后续明确指示在当前配置数据库创建本模块表；不插入业务数据。
- 用户后续明确要求自动建表，允许执行本模块DDL；不自动新增或运行单元、e2e 测试；验证使用已授权静态检查、编译与架构检查。
- 不自动提交 Git，不引入依赖。

## Review Focus

1. 同名接口不同 HTTP 方法，不能串用知识。
2. 相同知识多对象引用，正文去重但条件不能丢失。
3. 组件不存在、组件无知识、接口未匹配分别返回缺口。
4. 单个接口无法解析，不丢失其他查询结果。
5. 流程知识保留条件与待确认状态，不自动判定适用。

## Task 1：存储及接口契约

**Files:** 新增 database/component-knowledge.sql、docs/component-knowledge-api.md。

**Interfaces:** POST /api/v1/knowledge/component-context；请求 systemCode、projectCode、pagePath、componentId 必填字符串，interfaces 为可空数组，每项 method、address 必填；缺省或 null 按空列表。

响应沿用 Result，data 包含 knowledgeEntries、applicabilities、interfaceContexts、codeLocations、issues。知识正文按 knowledgeId 唯一；关系保留 targetType、targetId、condition、例外及替代依据。issues 保存对象身份和缺口类型。全部合法请求成功执行查询后 code=200，即使存在缺口；数据库故障走现有异常处理，不伪装为缺失。

- [ ] 建立项目、服务、页面、组件、流程、页面流程关系、接口、知识、适用关系表；全部使用 knowledge_ 前缀。每表主键、字段中文 COMMENT，无外键、无 JSON 类型。系统项目与联合组件身份、服务身份、接口身份设置唯一索引。
- [ ] 前端本地目录保存于项目；后端目录保存于服务。当前服务按单开发者本地部署处理，不建立多用户工作区管理。
- [ ] 写完整请求响应、字段类型与必填可空、业务口径、错误及缺口枚举、示例；接口路径模板支持整段 {name} 占位符，精确匹配优先，多模板命中返回歧义。
- [ ] 人工核对五类 Review Focus 对应的契约结果，不新增测试代码。数据库 MySQL 版本与字段索引长度在执行前核对，缺少数据库时明确未执行范围。

## Task 2：知识检索与汇总

**Files:** 新增 domain/knowledge/model 下 ComponentKnowledgeQuery、KnowledgeContext；domain/knowledge/support/KnowledgeDataSupport.java；infrastructure/knowledge/dao 下 mapper/KnowledgeMapper.java、po/ 下所需持久化对象、converter/KnowledgeDataConverter.java；infrastructure/knowledge/supportimpl/KnowledgeDataSupportImpl.java；resources/mapper/knowledge/KnowledgeMapper.xml。Java 路径均相对 src/main/java/com/cmbchina。

**Interfaces:** KnowledgeDataSupport.load(ComponentKnowledgeQuery query): KnowledgeContext。查询包含四项组件身份及解析后的接口身份；结果包含领域知识、适用关系、代码目录与缺口，不能直接返回 PO。

- [ ] 实现 XML 批量查询，组件与接口两条查询链独立；组件缺失仍查询输入接口。只沿页面、组件、页面关联流程及明确接口关系取知识。
- [ ] 读取已确认及待确认知识，失效项默认排除；显式引用到失效知识时返回提示。适用条件缺失不得当作无条件。
- [ ] 正文按 ID 去重，关系不因正文去重丢失，按稳定 ID 排序以获得确定输出；不按时间或对象层级裁决冲突。
- [ ] 核对五类 Review Focus 的 SQL 过滤条件与汇总逻辑；关联超过三表时说明关联及索引，具备数据库和代表数据后执行 EXPLAIN，否则明确未验证性能。

## Task 3：查询入口与接口解析

**Files:** 新增 application/knowledge/api/ComponentKnowledgeApplicationService.java；application/knowledge/api/dto/ 下 ComponentKnowledgeRequest、InterfaceKnowledgeRequest、ComponentKnowledgeResponse 及响应子对象；application/knowledge/service/ComponentKnowledgeApplicationServiceImpl.java；application/knowledge/support/InterfaceAddressParser.java；adapter/knowledge/web/ComponentKnowledgeController.java。修改 src/main/resources/application.yml，仅将 mapper-locations 改为 classpath:mapper/**/*.xml，保留报表 XML 加载。

**Interfaces:** ComponentKnowledgeApplicationService.query(ComponentKnowledgeRequest request): ComponentKnowledgeResponse；InterfaceAddressParser.parse(String method, String address) 返回解析结果或缺口；Controller 只校验和委派。

- [ ] 采用 javax.validation 校验联合标识和接口项，使用独立请求响应 DTO。
- [ ] 地址必须为 /服务/实际路径；去掉查询参数后解析第一段服务标识，保留实际路径。方法规范化大写；非法单项作为缺口，不影响其他接口。限制为路径输入，不额外支持完整 URL。
- [ ] 编排领域查询，转换为文档契约，返回前后端地址和全部适用关系；不读取本地源码、不执行规则判断。
- [ ] 同步 Task 1 接口文档，检查五类 Review Focus 逐项行为与实现一致。

## Task 4：必要验证与交付

**Files:** 上述改动文件及 docs/component-knowledge-api.md。

- [ ] 执行 bash scripts/java-quality.sh verify，参数为本次新增的 adapter/knowledge、application/knowledge、domain/knowledge、infrastructure/knowledge Java 目录；失败只修正本次代码后重跑。
- [ ] 执行 bash scripts/backend-gate.sh src/main/java/，确认编译和架构检查结果；不擅自运行测试，先核对脚本不会运行未授权测试。
- [ ] 核对 YAML mapper 路径兼容现有报表；检查 SQL 与 XML 字段一致、文档示例与 DTO 一致、Markdown 引用有效。
- [ ] 无数据库结构或数据时明确接口数据库查询、EXPLAIN 和真实业务结果未验证，不宣称完整运行验证通过。不自动执行 DDL 或添加模拟回退。

## 自审

设计范围映射：存储及适用关系由任务 1、2覆盖；接口解析与代码地址由任务 2、3覆盖；缺口与冲突由任务 1—3覆盖；检查由任务 4覆盖。录入、版本、开发部署不进入任务。上述传输字段、路径模板规则和单开发者地址映射是实施建议，随计划确认，不擅自扩展已批准设计。
