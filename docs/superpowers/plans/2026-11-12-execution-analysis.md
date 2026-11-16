# 执行视角分析模块 Implementation Plan

> 此为前期文档计划。用户后续已授权接口开发，确认topN可选默认5、在当前目录保留已有改动；当前开发以 [接口实施计划](2026-11-12-execution-analysis-api.md) 为准。

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task; use superpowers:subagent-driven-development only if the user chooses delegation. Steps use checkbox syntax for tracking.

**Goal:** 将已批准的三个模块设计转为可实施、可核对的数据需求和交互说明，不在本次设计任务中编写产品代码。

**Architecture:** 沿用现有执行报表的筛选、每日最新执行、客户去重和机构基准规则。趋势复用四项客户率统计，技能参与独立核实未执行任务人群，关注支行复用支行统计。接口开发与 HTML 修改属于后续实施范围，不由设计批准自动推定授权。

**Tech Stack:** 现有原型为单文件 HTML/CSS/JavaScript；服务端已有 Java 执行统计模块。本计划交付 Markdown 需求及契约建议，不新增依赖。

**Spec:** `docs/knowledge/README.md`

## Global Constraints

- 不修改产品代码、原 HTML、现有接口契约或其他模块。
- 不新增日志、权限、导出、监控及无关 UI。
- 未经授权不新增或运行测试，不自动提交 Git。
- 数据无法证明动作先后或因果关系时，仅并列诊断，不构造查看后接触转化。
- 本计划以文档核对替代自动测试步骤；不得据此声称运行验证通过。

## Review Focus

1. 单日与超过七日区间必须完整覆盖，不能继续固定七日。
2. 同客户跨日重复，日指标分别去重，区间指标整体重算。
3. 零分母返回 null；接触率超过 100% 保留，不截断。
4. 收到任务但未执行的人必须进入技能分母，不能被执行数据过滤掉。
5. 关注支行超过四家时总数仍为全部命中数，基准取消支行、等级和技能过滤。

## 文件与现有边界

- 新增 `docs/report-execution-analysis-requirements.md`：三个模块的业务口径、数据字段建议、边界和验收示例。
- 仅引用 `docs/report-execution-requirements.md`、`docs/report-api-requirements.md`：沿用公共规则，不改动稳定模块口径。
- 设计与计划文档仅同步已核实事实及审批状态。
- 后续开发可复用 `ExecutionReportApplicationService`、`ExecutionStatisticsService` 和已有支行响应；本计划不添加方法、DTO、Mapper 或路由。技能分母数据条件未解决前不锁定查询实现。

### Task 1: 明确走势数据需求

**Files:** Create `docs/report-execution-analysis-requirements.md` 的走势章节。

**Interfaces:** 输入沿用 startDate/endDate/bbkId/orgId/level/skillIds；建议输出 dailyPoints，每点包含 date、recommendedCustomerCount、viewCustomerCount、insightCustomerCount、callCustomerCount、contactCustomerCount 及对应四项 rate。日期使用 yyyy-MM-dd，数量为非负整数，率为可空百分比数值；此为契约建议，非已发布接口。

- [ ] 写明包含首尾、逐日升序、无数据日期保留，四项率均以当日推荐客户为分母。
- [ ] 复制引用现有 insight/phone 按钮筛选及客户匹配规则；明确查看为 is_read 对应推荐客户去重，接触沿用既有规则。
- [ ] 写出核对示例：推荐10户、查看8户、洞察6户、电访4户、接触2户，对应80/60/40/20；推荐0户则四率均为null。
- [ ] 核对单日、连续十日、相同客户跨两日及接触率120%的说明；不将示例作为真实查询结果。

### Task 2: 核实并定义技能参与数据边界

**Files:** Modify `docs/report-execution-analysis-requirements.md` 的技能章节。

**Interfaces:** 相同顶部筛选输入，增加 topN 和 sortKey。sortKey 支持 participantCount、participationRate、assignedManagerCount，默认 participantCount；用户已确认topN可选，未传或null默认5，显式值为正整数。输出 skillId、skillName、participantCount、assignedManagerCount、participationRate。按实际任务关联人员与技能，人员使用 sap_id 去重。

- [ ] 仅查看现有人员—规划—技能—任务—排程关联和执行 is_read 映射，确认能否保留无执行记录的任务人员；不访问无关源码或变更数据库。
- [x] 用户已确认：存在人员技能任务及区间排程即收到任务，该任务执行is_read=1即归属该人员参与；分母包含未执行人员，不能从已执行数据集反推。
- [ ] 在确认后写明参与人数必须来自分母人群、同技能同人员仅计一次。TOPN由topN入参确定，按sortKey指定的参与人数、参与率或应执行人数降序，默认参与人数；同值按技能编号升序，参与率使用未舍入值排序，先排序再截取。
- [ ] 核对三种排序、默认排序、同值排序、N超过技能总数及非法topN/sortKey；非整数、超出整数范围、零或负数topN返回现有参数错误，缺失或null默认5；卡片标题应随请求N更新，不写死TOP5。文档明确不新增错误码，不指定额外最大N。
- [ ] 核对示例：技能A有3人收到任务，1人无执行、1人执行未查看、1人重复查看，参与为1/3，展示33.33%；零分母不入榜；同一人参与技能B不影响A去重。

### Task 3: 定义关注支行及交互

**Files:** Modify `docs/report-execution-analysis-requirements.md` 的关注支行、交互与验收章节。

**Interfaces:** 消费现有支行统计与整体基准；建议输出 totalCount、rows（最多四条），每条保留分支行组合编号、查看与接触客户数/率/百分点差和诊断类型。点击复用现有客户经理明细入参 filters/scopeBbkId/scopeOrgId。

- [ ] 明确三种诊断：仅查看低、仅接触低、两者均低；严格低于基准才命中，不采用支行明细颜色标识的±3pp阈值。
- [ ] 写明零推荐不排名；按接触落后百分点降序，再按推荐客户数降序、分支行组合编号升序；仅查看偏低者随后按查看落后程度排序，平局规则相同。
- [ ] 核对基准为空时对应指标不参与判断；比较使用未舍入率值，显示两位小数；totalCount在截取四条之前计算。
- [ ] 写明走势四条线、悬停客户量与率值、保留客户量切换、长区间减少标签但不丢日数据；技能展示人数分子分母；支行下钻继承筛选，不新建页面或管理动作。
- [ ] 核对至少五家命中、基准相等、小样本、组合机构编号重复及空列表示例。

### Task 4: 文档验收与后续实施边界

**Files:** Review 上述需求文档及已批准设计。

- [ ] 逐项对照设计，确认三模块覆盖、命名一致、例子可按公式复算、引用文件存在；核对未决项未被写成已确认结论。
- [ ] 对本次文档执行 `git diff --check`，并单独检查新增未跟踪文档的空白与引用；仅报告实际结果，不执行编译、服务启动或测试。
- [ ] 交付需求文档及数据核实结果，列出尚未解决的分母问题。没有任务接收口径时不得宣称技能模块已具备开发条件。
- [ ] 如用户另行要求开发，再读取适用后端规范、确认原 HTML 的交付位置，并编写包含确切路由、DTO、查询文件及静态检查命令的代码实施计划；接口变更须同步完整接口说明。

## 自审结论

四项走势、技能实际任务关联、支行诊断、全部边界均有对应任务。当前仅授权调研设计，因此不安排产品代码、自动测试或提交；技能分母及人员归属口径现已确认，不以已执行人员替代。

## 执行记录

- 已交付 `docs/report-execution-analysis-requirements.md`，完成任务1及任务3的需求定义，覆盖四项每日走势、诊断、排序、空值和示例。
- 任务2已完成现有关系模型、执行模型、Mapper及应用服务的数据核实；用户随后确认任务关系加区间排程即收到任务、is_read归属目标人员，相关业务确认阻塞解除。后续开发需要保留未执行任务人群，现有执行数据集不足以计算完整分母。
- 任务4已执行 `git diff --check`；进一步核对新增文档空白及引用。未执行单元测试、e2e、编译或服务启动，因为此次仅修改文档。
- Ruling: 沿用计划的文档交付范围，不将“实施”扩展为产品代码开发；不创建隔离工作树、测试脚本、提交或额外执行工具文件，避免超出本次文档任务。
- 后续用户明确答复：topN可选，未传默认5；在当前目录继续，保留已有改动。
- 独立文档审阅未发现重要矛盾；已补明支行率差值为支行率减基准率，负值表示落后。
