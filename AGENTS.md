

# AGENTS.md

本目录为通用规范模板。将 AGENTS.md、docs/standards/、config/、scripts/、tools/ 和 .codex/ 一并复制到目标项目根目录使用。目录、技术栈、版本和命令以目标项目实际配置为准；框架专项规则仅在使用该框架时适用，不为套用规范引入依赖。

## 禁止项执行要求（必须遵守）

**禁止项为硬性约束，修改前识别，交付前核对。**


- 修改前：识别当前任务适用的禁止项，作为实现边界；专项规范仍按需读取；实现中：发现方案触碰禁止项，先改用合规方案；无法满足时明确说明冲突；交付前：核对本次改动是否触犯禁止项，发现违规先修正；例外仅按规则中已明确的条件或用户明确指示执行。

- ❌ 禁止自行放宽、绕过或违反禁止项。
- ❌ 禁止在存在未修正的违规改动时宣称任务完成。
- ❌ 禁止将推测或默认行为视为用户授权。

## 适用范围与优先级

- 本文适用于采用本规范的项目，按全局、后端、数据库、前端和交付要求分类；顶层编码原则优先遵守；同一事项的具体规则优先于通用规则；Java 方法参数上限按后端的 6 个执行；测试相关的目标与示例不构成新增或运行测试的授权，仍须遵守全局约束。

## 📌 Top-level Coding Guidelines（优先遵守，高于其他规范）
### 1. Think Before Coding
**Don't assume. Don't hide confusion. Surface tradeoffs.**
Before implementing:
- State your assumptions explicitly. If uncertain, ask.
- ❌ If multiple interpretations exist, present them - don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

### 2. Simplicity First
**Minimum code that solves the problem. Nothing speculative.**
- ❌ No features beyond what was asked.
- ❌ No abstractions for single-use code.
- ❌ No "flexibility" or "configurability" that wasn't requested.
- ❌ No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.
Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

### 3. Surgical Changes
**Touch only what you must. Clean up only your own mess.**
When editing existing code:
- ❌ Don't "improve" adjacent code, comments, or formatting.
- ❌ Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- ❌ If you notice unrelated dead code, mention it - don't delete it.
When your changes create orphans:
- Remove imports/variables/functions that YOUR changes made unused.
- ❌ Don't remove pre-existing dead code unless asked.
The test: Every changed line should trace directly to the user's request.

### 4. Goal-Driven Execution
**Define success criteria. Loop until verified.**
Transform tasks into verifiable goals:
- "Add validation" → "Write tests for invalid inputs, then make them pass"。
- "Fix the bug" → "Write a test that reproduces it, then make it pass"。
- "Refactor X" → "Ensure tests pass before and after"。
For multi-step tasks, state a brief plan:
```
1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]
```
Strong success criteria let you loop independently. Weak criteria ("make it work") require constant clarification.


## 全局基础约束
- 工作根目录为当前项目根目录，文件读写和 shell 操作限定在任务授权范围内；只读取当前任务必须依赖的文件；一次只处理当前指令指定的需求；允许为完成当前任务自动执行必要的编译、服务启动及重启，无需逐次确认；相关 shell 命令可直接执行。
- 单元测试、e2e 测试及其他未授权的 shell 操作仍需先征求确认；完成必要验证后停止，等待下一步指令；输出优先最小可用实现，注释只保留业务必要注释，减少冗余描述，避免不必要美化。

- ❌ 禁止一次性读取大量无关源码。
- ❌ 禁止默认读取或修改忽略项：`node_modules、dist、target、.git、logs、cache、tmp、.idea、.vscode`。
- ❌ 禁止主动扩展功能、自动追加配套代码（如自动补测试、自动加权限、自动写docker/CI脚本、自动新增其他页面/接口），除非我单独说明。
- ❌ 禁止无限制连续工具自循环。

## 专项规范（按需读取）

修改前按任务范围读取，范围扩大时补读：

- 后端、SQL、数据库：[后端规范](docs/standards/backend.md)。
- 前端：[前端规范](docs/standards/frontend.md)。
- 跨端任务读取两份；通用文档任务无需读取，修改专项规范时读取对应文件。
- 技术栈、目录及命令以项目实际配置为准。

- ❌ 禁止加载与任务无关的专项规范。
- ❌ 禁止为套用规范引入或替换技术栈。

## 行为红线（任何时候不能主动做）

- ❌ 禁止主动新增登录、token鉴权、权限模块（指令明确要求才做）。
- ❌ 禁止主动增加操作日志、导出导入、埋点监控。
- ❌ 禁止主动美化UI、增加动画、自定义主题。
- ❌ 禁止一次性同时修改多个无关模块、跨模块自动联动改动。
- ❌ 禁止自行臆测潜在bug并批量修复。
- ❌ 禁止修改当前任务授权范围之外的稳定业务代码。
- ❌ 禁止过度设计、提前预留不需要的扩展能力、增加未要求的抽象层。

## 完成交付前检查（强制）

项目初始化还必须按 [INIT.md](scripts/INIT.md) 启动适用的前后端服务，验证后端质量门禁及 hook 实际执行；缺少环境或 hook 尚未信任时明确报告未完成事项。


项目已启用并信任 hook 时自动检查，接入见 [HOOKS.md](scripts/HOOKS.md)；未启用时仍须手动执行，不能因存在配置文件就视为已检查。


- 修改代码后，必须对本次新增、修改的代码执行适用的代码质量与静态检查；Java 执行后端规范中的 verify，并运行目标项目编译与架构检查，其他语言使用项目已配置的 lint、类型检查等命令。
- 检查属于任务必要验证，可直接执行；修正检查问题后重新执行，最终输出实际命令、检查范围和结果。
- 新增、修改或删除接口设计与实现时，必须在同一任务中同步更新项目接口文档，覆盖接口路径、HTTP 方法、请求参数、响应字段、类型、必填/可空、校验与错误码、业务口径及请求响应示例等本次变更涉及的内容；交付前核对文档与当前设计、实现一致。
- ❌ 禁止接口契约已变更但接口文档未同步时宣称任务完成。
- 仅文档变更检查内容及引用；工具或配置变更验证其实际执行效果。缺少工具或配置时明确报告阻塞及未验证范围。

- ❌ 禁止未执行检查或检查未通过时宣称代码任务完成。
- ❌ 禁止将工具缺失、编译通过或人工阅读视为静态检查通过。

## 每次完成任务输出模板
```
✅ 当前任务：{当前任务描述}
✅ 已完成变更：
- 文件改动清单
- 验证结果及未解决事项（按实际结果说明）
💡 待确认/可选后续动作（由你决定是否执行）：
- [ ] xxx
- [ ] xxx
❓ 需要确认的问题（如有）：
```
