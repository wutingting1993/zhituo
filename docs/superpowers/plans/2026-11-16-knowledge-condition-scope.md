# Knowledge Condition Scope Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** 在现有 conditions 中保留当前查询的适用关系身份和范围。

**Architecture:** 复用 SQL 已返回的关系信息，在当前条件组装处补充字段。保持知识正文去重与其他组件反向引用契约。

**Tech Stack:** Java 8、Spring Boot 2.7.2、Lombok、MyBatis。

**Spec:** docs/superpowers/specs/2026-11-16-knowledge-condition-scope-design.md

## Global Constraints

- 不改数据库、前端、独立模型上下文接口或 relatedComponents 契约。
- 不新增或运行测试；仅执行必要静态检查、编译和架构检查。
- 不自动提交，不修改无关历史代码。

## Review Focus

- 同一知识由 PAGE 和 COMPONENT 同时命中时保留两条条件。
- 相同条件内容、不同关系 ID 不合并；同一关系 ID 不重复。
- 其他组件的引用不进入当前 conditions。
- 条件、例外、替代字段及空值序列化行为保持原样。
- targetId 是关联对象数据库主键，不能误用页面内组件标识。

---

### Task 1: 条件范围字段与接口文档

**Files:**
- Modify: src/main/java/com/cmbchina/application/knowledge/api/dto/KnowledgeCondition.java
- Modify: src/main/java/com/cmbchina/application/knowledge/support/KnowledgeContextAssembler.java
- Modify: docs/component-knowledge-api.md

**Interfaces:**
- Consumes: 当前关系项 getId()、getTargetType()、getComponentId()，其中 getComponentId() 在临时当前关系集合中承载 targetId。
- Produces: conditions[].id、targetType、targetId 三个 string 字段；原字段保持。

- [x] 为 KnowledgeCondition 增加三个 String 字段，保持现有 Lombok 风格。
- [x] 修改 remember 方法，按关系 ID 去重，将 id、targetType、targetId 连同原条件字段一起构建；只从反向引用替换前的当前关系读取。
- [x] 更新接口文档的 conditions 定义、去重依据、适用语义，并加入 PAGE/COMPONENT 同时命中的说明性 JSON 示例。
- [x] 人工逐项核对 Review Focus；不运行测试。
- [x] 执行 `bash scripts/java-quality.sh verify src/main/java/com/cmbchina/application/knowledge/api/dto/KnowledgeCondition.java src/main/java/com/cmbchina/application/knowledge/support/KnowledgeContextAssembler.java`，要求退出码 0。
- [x] 核对 JDK/Maven 版本后执行 `mvn -B -Dmaven.repo.local="$PWD/.tool-cache/m2" compile` 与 `python3 scripts/architecture-check.py .`，要求退出码 0；如质量脚本使用的仓库路径不同，采用项目既有配置。
- [x] 检查文档引用与变更范围，报告实际命令、结果及未验证项。

## 执行记录

2026-11-16：按用户确认在当前目录直接实施，不创建额外工作区或提交。两个 Java 文件的 verify 退出码 0（0 Checkstyle violations，存在原有作者/日期提示）；JDK 17 下 Maven compile 及 architecture-check.py 均退出码 0。架构检查内部使用工具自身的 ArchitectureTest；未新增或运行业务单元测试、e2e 或接口请求验证。当前关系在反向引用替换前读取，其他组件关系不进入 conditions。
