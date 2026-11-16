# 项目知识维护落地计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在当前项目启用会话任务记录和用户指定任务组的知识归档流程。

**Architecture:** AGENTS.md 作为生效入口，引用独立维护规则；知识入口区分正式知识、现有资料与未归档任务。只有主动归档才将任务知识合并为正式知识。

**Tech Stack:** 仓库 Markdown 和相对链接，无新增依赖。

**Spec:** ../specs/2026-11-11-report-knowledge-design.md

## Global Constraints

- 不修改业务代码、数据库或接口，不自动提交 Git。
- 不新增或运行单元、e2e 测试，不安装后台任务、hook或全局技能。
- 存量语义变更须明确确认；纯新增仅在用户主动归档时自动合并。
- 当前应用机制不等于已授权归档历史报表任务；不提前迁移现有业务口径。

## Review Focus

- 新会话能从 AGENTS.md 定位入口和未归档任务。
- 未归档结论不会冒充正式规则，历史资料不会冒充已核实知识。
- 指定任务组不明确时澄清；冲突不按时间自动裁决。
- 部分归档和重复归档有明确记录，不重复正文。
- 任务完成不等于业务确认、实现或验证全部完成。

## Task 1：启用项目指引与知识入口

**Files:** 修改 AGENTS.md；新增 docs/knowledge/README.md、docs/knowledge/maintenance.md。

**Interfaces:** 消费批准设计；产出项目代理入口、阅读路径和维护规则。

- [ ] 在 maintenance.md 写入会话触发、任务最小字段、批量归档步骤、审批边界、恢复与并发规则；不复制整份设计。
- [ ] 建立 README.md，链接维护规则、任务目录及现有三个报表需求、接口和验证资料，注明它们尚未完成正式知识归档。
- [ ] 在 AGENTS.md 增加简短的必要读取与维护指令，明确用户主动指定任务组才触发正式归档。
- [ ] 人工从 AGENTS.md 沿链接走到入口及维护规则，检查上述五种边界均被覆盖。

## Task 2：为当前任务建立持久化记录并检查

**Files:** 新增 docs/knowledge/tasks/README.md、docs/knowledge/tasks/2026-11-11-knowledge-maintenance.md。

**Interfaces:** 使用 Task 1 的任务字段及归档规则；产出可恢复的当前任务记录和任务索引。

- [ ] 建立任务索引，说明标识、状态、来源和新增任务登记方式；不为未读取的历史会话补造记录。
- [ ] 记录本任务已确认边界、设计和计划链接、实际落地文件、验证结果及待归档状态。
- [ ] 检查新增修改 Markdown 的相对链接均能定位文件，核对内容无即时写入长期知识的旧规则。
- [ ] 按纯新增、存量未批准变更、组内冲突、重复归档和会话恢复五种情形人工推演指引，报告结果；不冒充真实跨会话执行验证。
- [ ] 交付文件清单与边界，保留历史报表资料待用户指定归档。

## 自检

计划覆盖指引接入、任务记录、归档流程和文档验证。当前仅启用机制，不执行历史资料归档，因此不创建公共指标、模块正文或变更空目录。首次实际归档时按指定任务范围创建必要文件。
