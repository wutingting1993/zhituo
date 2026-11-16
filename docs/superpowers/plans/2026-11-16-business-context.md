# 用户视角组件上下文 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为模型上下文增加可阅读、可追溯的业务说明，支持理解现状、需求澄清和编码。

**Architecture:** 在现有应用层数据投影后确定性渲染业务说明，保留结构化契约。复用已读取知识和接口关系，不新增查询或语义推断。

**Tech Stack:** Java 8、Spring Boot 2.7.2、Lombok、Guava、Jackson，沿用现有 MyBatis/MySQL。

**Spec:** `docs/superpowers/specs/2026-11-16-business-context-design.md`

## Global Constraints

- 只调整模型上下文及相关文档，不修改 summary、前端知识接口、数据库、录入方式。
- 不新增依赖、模型调用、权限、监控、会话或未要求的抽象层。
- 不新增或运行单元/e2e测试；静态检查、编译、架构检查属于必要验证。
- 保留原始正文、状态、条件、例外、引用、空值约定及查询范围；不按标题猜测含义。
- 不自动提交 Git；不读取忽略项。

## Review Focus

- PENDING 或 CONFLICT：应完整展示分歧，不能变成已批准规则。
- 无条件范围：不得把缺失 conditions 解读为无条件；保留范围未知提示。
- 部分详情：说明只覆盖本次请求，不暗示组件知识完整。
- 原文含 Markdown、空描述、未知分类：不破坏章节，保留事实并标明缺失。
- summary 新需求决定：不得混入当前能力，尤其技能状态重叠尚未实现。

上述情况用文档案例和人工逐项核对，不擅自新增自动化测试；运行时验证尚需授权。

---

### Task 1: 透出既有知识分类

**Files:**
- Modify: `src/main/java/com/cmbchina/application/modelcontext/api/dto/ModelKnowledge.java`
- Modify: `src/main/java/com/cmbchina/application/modelcontext/support/ModelKnowledgeAssembler.java`
- Modify: `src/main/java/com/cmbchina/application/modelcontext/service/ModelContextApplicationServiceImpl.java`
- Modify: `docs/model-context-api.md`

**Interfaces:** 消费 KnowledgeItem.getKind()；ModelKnowledge 新增 KnowledgeKind kind，缺失时沿现有 NON_NULL 省略。

- [ ] 在 ModelKnowledge 增加 kind，沿 Lombok 和现有 DTO 风格。
- [ ] 在 full(KnowledgeItem) 逐值映射，在 fillKnowledge 的技术目录投影中保留同一分类；不改变 collect 的条件与正文去重。
- [ ] 同步首包与详情分类字段的类型、枚举、可缺省含义及示例。
- [ ] 核对 CONFLICT 不依赖标题识别，未知分类不丢弃知识、不推断类别。

### Task 2: 组织业务说明并接入两个入口

**Files:**
- Create: `src/main/java/com/cmbchina/application/modelcontext/support/ModelBusinessContextAssembler.java`
- Modify: `src/main/java/com/cmbchina/application/modelcontext/api/dto/ModelComponentResponse.java`
- Modify: `src/main/java/com/cmbchina/application/modelcontext/api/dto/ModelDetailsResponse.java`
- Modify: `src/main/java/com/cmbchina/application/modelcontext/service/ModelContextApplicationServiceImpl.java`
- Modify: `docs/model-context-api.md`

**Interfaces:**
- `String component(ModelComponentResponse response)`：在 interfaces 和 issues 组装完成后调用，消费响应中已有业务正文、分类、条件、接口及缺口。
- `String details(ModelDetailsResponse response)`：仅 knowledgeIds 非空且 knowledge 已赋值后调用；处理空命中、范围未知和实际返回子集。
- 两个响应增加 String businessContext；首包始终返回，详情仅明确请求知识时返回。

- [ ] 新增应用层 Spring 组装组件，两个公开方法按设计固定章节输出 String；用 StringBuilder 直接组织，无额外抽象层。
- [ ] 原文逐值保留并固定缩进，知识带 ID、状态、条件、例外及替代引用；分类使用 kind，不解析正文推导关系。
- [ ] 首包渲染页面/组件描述、业务知识、分歧、缺口、接口身份和已有规则 ID 关系；技术正文保留按需获取。
- [ ] 详情渲染请求子集，不额外请求数据；未知范围明确提示，不自动扩展当前知识范围。
- [ ] 服务注入组装器，在两个响应的资料与 issues 均设置完成后生成说明，不修改请求校验和 Controller。
- [ ] 同步两个接口新增字段、缺省条件、章节含义、引用方式及体积取舍。
- [ ] 人工核对 Review Focus 五项、首包无业务知识、详情空命中和仅 codeLocations 请求的预期行为。

### Task 3: 示例、验收与必要检查

**Files:**
- Modify: `docs/model-context-real-example.md`
- Modify: `docs/model-context-api.md`

**Interfaces:** 消费 Task 1 的 kind 和 Task 2 的 businessContext，示例必须与最终 DTO 和实际渲染规则一致。

- [ ] 添加明确标为说明性示例的新增输出；既有真实调用记录不改写成新接口实测结果，不伪造 HTTP 验证。
- [ ] 用用户指定的 summary 附件核对设计验收点，将本轮技能需求决定与组件现状明确区分；不读或修改 summary 实现。
- [ ] 核对所有修改文件与本任务直接相关、接口文档覆盖新增字段和按需约定。
- [ ] 执行 `java -version`、`mvn -version`，确认工具环境符合项目要求；缺失时报告阻塞，不升级项目版本。
- [ ] 执行 `bash scripts/java-quality.sh verify --changed`；修正仅本次引入的问题后重跑，退出码 0 才通过。
- [ ] 执行 `bash scripts/backend-gate.sh src/main/java`，包含静态检查、格式检查、`mvn clean compile` 和架构检查；不调用测试脚本。
- [ ] 若全量门禁受历史问题阻塞，报告具体范围，不将编译通过当作静态检查通过。
- [ ] 交付列出实际修改、检查命令、结果及未验证范围：未授权的 HTTP 调用、模型效果实验和单元/e2e测试均不执行。

## 计划自查

已覆盖业务说明、技术依据、溯源、状态和条件保留、部分详情、文档同步与必要检查。没有实现模型推理工作流，只有上下文说明及使用约定；不承诺模型遵循指引或自动减少澄清错误。未登记业务关系保持未知。实施前由用户审阅具体字段与渲染方式。
