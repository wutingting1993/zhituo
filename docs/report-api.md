# 智拓报表接口文档

> 版本：v1 · 更新日期：2026-11-13 · 依据：当前 Controller、DTO 和应用服务实现。

## 1. 通用约定

- 基础路径：`/api/v1/reports`，部署地址由环境提供。
- 协议：HTTP/HTTPS；所有接口使用 `POST`，请求和响应均为 UTF-8 JSON。
- 请求头：`Content-Type: application/json`；建议 `Accept: application/json`。
- 所有接口必须提供 JSON body，检索全部候选项时传 `{}`；不从 URL 查询参数获取检索条件。
- 当前代码未声明认证头或认证机制，本接口文档不新增认证要求。
- 接口均只读，不提供分页；数组无数据返回 `[]`，数量无数据返回 `0`。
- 必填日期及必填下钻机构不因空字符串规则变为可选；`skillIds` 不使用字符串 `""` 代替数组，不新增数组元素过滤。非空字符串保持原样，不自动去除空格。
- 日期格式 `yyyy-MM-dd`，指排程 execute_day，含开始日和结束日，开始日不得晚于结束日。
- 统计接口先按所选排程日期及技能筛选规划与技能组合，再查询目标人员和任务；无候选排程时返回空统计。后续仍按具体排程当天匹配执行，要求执行时间不早于规划创建时间，每任务每天选取最新执行后按 trace_id 回查；不新增规划有效期或发布状态限制。filters 候选项不受此日期筛选影响。
- 分行、支行、等级单选，技能多选取或；可选字符串字段未传、null或空字符串 `""` 均按未传处理；技能使用数组，`[]` 表示未传，仍兼容省略或null。编号用于筛选和去重，名称仅展示。
- 查看率、洞察率、电访率、接触率均以推荐客户数为分母；例如 `62.5` 表示 `62.5%`，分母为0时为null，展示为“—”。
- 百分比、百分点差值保留两位小数，人均值四舍五入为整数，户/任务保留一位小数。JSON数值不保证保留尾随零。接触率可能超过100%，流失率可以为负。
- null与字段缺失含义不同：响应中的可空字段按实际序列化结果显式返回null。机构名称、人员名称和等级等来自可空数据库字段，可能为null。
- 各层级独立去重，不以每日、人员、技能或下级机构数值直接累加替代汇总。
- 所有接口均不校验分行与支行的归属关系；不匹配时正常执行查询，数量为0、列表为空，不返回机构归属错误。下钻必填、日期及“下钻机构超出顶部检索范围”的校验仍保留。机构记录不存在时，下钻响应保留请求编号，机构名称为null。filters按各候选项的独立联动规则返回，不保证所有候选数组同时为空。

## 统计实现说明

所有报表统计接口现由显式JOIN及非递归CTE在数据库中完成范围筛选、去重及聚合；Java仅接收汇总行及技能展示选项，负责比率、精确比较、排序、TOPN、响应组装和日期补零。filters仍直接查询候选结果。总体和机构基准独立聚合，不累加分组客户数。

规划覆盖和来源结构保留只统计已执行记录的行为；规划明细保留待执行任务。参与度单独计算有区间排程的任务分母，不要求执行或成功结果。相同维度编号有多个展示值时采用最小值确定展示，替代此前无稳定顺序的首条值。

本机423项功能对照通过，详见[SQL聚合功能验证](report-sql-validation.md)。未执行性能测试，不据此承诺生产规模响应时间或并发上限。

## 2. 统一响应与结果判断

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| code | integer | 业务码；200成功 |
| message | string | 可展示的结果说明 |
| success | boolean | 成功true，失败false |
| data | object或null | 成功数据对象；失败为null |

应用可控制的正常与异常 JSON 响应 HTTP 状态均为200；客户端应判断code和success。网关、容器等应用外部错误不保证该结构。

结果判断：`code=200` 表示成功，其他值均表示失败；失败原因查看 `message`，失败时 `data=null`。不应按错误文案编写客户端判断逻辑。

## 3.1 检索选项

**请求**：`POST /api/v1/reports/filters`

**请求字段**

“必填”包括字段校验和业务校验；使用点号表示嵌套字段，`[]` 表示数组元素。

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `bbkId` | string | 否 | 分行编号；请求未传、null或空字符串表示全部 |
| `orgId` | string | 否 | 支行编号；请求未传、null或空字符串表示全部 |
| `skillIds` | `array<string>` | 否 | 技能编号数组，多选取或；未传、null或空数组表示全部 |

**请求示例**

```json
{
  "bbkId": "B01",
  "orgId": "O01",
  "skillIds": [
    "S01"
  ]
}
```

**响应字段**

响应表的“可空”描述字段值；嵌套字段仅在父对象非null或数组存在元素时适用。

| 字段 | 类型 | 可空 | 说明 |
| --- | --- | --- | --- |
| `code` | integer | 否 | 业务码，成功为200 |
| `message` | string | 否 | 结果说明 |
| `success` | boolean | 否 | 成功为true |
| `data` | object | 是 | 成功时为下列业务对象，失败时为null |
| `data.branches` | `array<object>` | 否 | 分行数组 |
| `data.branches[].id` | string | 是 | 编号，按编号识别对象 |
| `data.branches[].name` | string | 是 | 展示名称 |
| `data.branches[].subBranches` | `array<object>` | 否 | 支行候选项数组 |
| `data.branches[].subBranches[].id` | string | 是 | 编号，按编号识别对象 |
| `data.branches[].subBranches[].name` | string | 是 | 展示名称 |
| `data.skills` | `array<object>` | 否 | 技能数组 |
| `data.skills[].id` | string | 是 | 编号，按编号识别对象 |
| `data.skills[].name` | string | 是 | 展示名称 |
| `data.levels` | `array<string>` | 否 | 客户经理等级字符串数组 |

**成功响应示例**（编号及名称为占位值）：

```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "branches": [
      {
        "id": "B01",
        "name": "示例分行",
        "subBranches": [
          {"id": "O01", "name": "示例支行一"},
          {"id": "O02", "name": "示例支行二"}
        ]
      }
    ],
    "skills": [
      {"id": "S01", "name": "示例技能"}
    ],
    "levels": ["L1", "L2"]
  },
  "success": true
}
```

**返回结构变化**：移除顶层 `data.subBranches`，支行位于 `data.branches[].subBranches`。技能和等级保持独立列表；同一个支行编号出现在不同分行时，前端使用分行编号与支行编号的组合识别机构。无候选项时对应数组返回 `[]`。

**规则**：

| 返回数据 | 来源与条件 |
| --- | --- |
| `branches`及`subBranches` | 人员表全量机构去重，不受bbkId、orgId、skillIds或日期影响；按分行编号分组嵌套支行 |
| `skills`（未选分行、支行） | 技能表source_id=RMASSIST，排除空技能编号，按skill_id去重 |
| `skills`（已选分行或支行） | 规划bbk_id=bbkId、bbk_org_id=orgId（仅应用已传条件）；规划、场景不限制deleted；场景scene_id=技能skill_id；任务plan_id=规划id且skill_ids整字段=scene_id，任务source_id=RMASSIST、status不为deleted；技能source_id=RMASSIST |
| `levels` | 人员表实际pst_lvl，排除NULL和空字符串，去重升序，不受机构/技能条件影响 |

filters使用bbkId和orgId联动技能，两者同时传时取交集，支持仅选分行或仅选支行。skillIds不限制技能候选项；机构树和等级始终全量返回。初次加载传 `{}`，机构变更时传 `{"bbkId":"B01","orgId":"O01"}`。未选机构时可跨全部机构选择任意技能。

人员表不关联目标、规划、场景、任务或执行表。分行按first_bbk_id去重，支行按first_bbk_id和org_id组合去重；排除空分行编号，空支行编号不生成子选项。名称为空不丢弃编号；同分支行组合的名称取非空名称最小值，分行展示取该分行按支行编号排序的首条机构记录名称。各层按编号升序排列。

技能名称取同编号下非空skill_name最小值。不限技能更新时间、删除状态、上下架状态及include_in_statistics，不限制规划及场景的deleted、规划发布状态和有效期；不要求有目标人员或执行；已选机构时必须匹配上述任务。机构条件使用规划表所属机构，非目标人员机构；无匹配规划场景或有效任务时skills返回 `[]`，不回退全部技能，不校验分支行归属。

统计接口仍以独立的bbkId、orgId、skillIds、level字段接收实际筛选值，统计口径不受本次候选项简化影响。

## 3.2 顶部统计指标

实现说明：顶部统计由数据库使用显式JOIN及CTE完成执行范围选择、去重与聚合，仅将汇总结果返回Java；Java计算比率、差值并组装响应。机构基准使用同一聚合查询独立计算。其他统计接口也已切换SQL聚合，详见统计实现说明。功能对照结果见[顶部SQL聚合验证](report-summary-sql-validation.md)。本次改造使用本机现有数据进行功能对照，性能尚未测试，不代表已获得性能提升。同一技能编号出现多个名称时，聚合结果选择名称最小值；此前明细查询未指定名称顺序，取首条名称不稳定。执行时间并列时按执行主键倒序选一条，仍属于原口径允许的任取一条。

**请求**：`POST /api/v1/reports/summary`

**请求字段**

“必填”包括字段校验和业务校验；使用点号表示嵌套字段，`[]` 表示数组元素。

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `bbkId` | string | 否 | 分行编号；请求未传、null或空字符串表示全部 |
| `orgId` | string | 否 | 支行编号；请求未传、null或空字符串表示全部 |
| `level` | string | 否 | 客户经理等级，使用数据库实际值 |
| `skillIds` | `array<string>` | 否 | 技能编号数组，多选取或；未传、null或空数组表示全部 |
| `startDate` | string(date) | 是 | 排程开始日期，yyyy-MM-dd，含当日 |
| `endDate` | string(date) | 是 | 排程结束日期，yyyy-MM-dd，含当日，不得早于开始日期 |

**请求示例**

```json
{
  "bbkId": "B01",
  "orgId": "O01",
  "level": "L1",
  "skillIds": [
    "S01"
  ],
  "startDate": "2026-11-01",
  "endDate": "2026-11-12"
}
```

**响应字段**

响应表的“可空”描述字段值；嵌套字段仅在父对象非null或数组存在元素时适用。

| 字段 | 类型 | 可空 | 说明 |
| --- | --- | --- | --- |
| `code` | integer | 否 | 业务码，成功为200 |
| `message` | string | 否 | 结果说明 |
| `success` | boolean | 否 | 成功为true |
| `data` | object | 是 | 成功时为下列业务对象，失败时为null |
| `data.planCount` | integer(int64) | 否 | 规划数，按规划编号去重 |
| `data.skillCount` | integer(int64) | 否 | 技能数，按技能编号去重 |
| `data.taskCount` | integer(int64) | 否 | 任务数；成功推荐客户方案数，仅统计plan类型成功最新结果，排除空客户标识，按(trace_id,custuid)去重 |
| `data.scheduledTaskCount` | integer(int64) | 否 | 应执行任务数；按目标客户经理工号、规划编号、技能编号、排程日期、任务编号去重，包含未执行任务；无数据为0 |
| `data.executedTaskCount` | integer(int64) | 否 | 已执行任务数；应执行任务中存在执行记录，失败也算已执行；无数据为0 |
| `data.pendingTaskCount` | integer(int64) | 否 | 待执行任务数；应执行任务中无执行记录，不区分未来和已到期；无数据为0 |
| `data.failedTaskCount` | integer(int64) | 否 | 失败任务数；已执行任务最新记录的status不为success；无数据为0 |
| `data.recommendedCustomerCount` | integer(int64) | 否 | 推荐客户数，非空custuid在当前范围整体去重 |
| `data.coveredSubBranchCount` | integer(int64) | 否 | 覆盖支行数，按(分行编号,支行编号)去重 |
| `data.mostUsedSkill` | object | 是 | 使用人数最多的技能；并列按技能编号升序取第一项，无数据为null |
| `data.mostUsedSkill.id` | string | 是 | 编号，按编号识别对象 |
| `data.mostUsedSkill.name` | string | 是 | 展示名称 |
| `data.mostUsedSkill.userCount` | integer(int64) | 否 | 该技能使用人数，按工号去重 |
| `data.averageTaskCount` | integer(int64) | 是 | 人均任务数，四舍五入为整数，无人员为null |
| `data.averageRecommendedCustomerCount` | integer(int64) | 是 | 人均推荐客户数，四舍五入为整数，无人员为null |
| `data.viewRate` | number | 是 | 方案被查看率，百分比数值 |
| `data.contactRate` | number | 是 | 接触率，百分比数值，可能超过100 |
| `data.benchmarkViewRate` | number | 是 | 机构方案查看基准率 |
| `data.benchmarkContactRate` | number | 是 | 机构接触基准率 |
| `data.viewRateDifference` | number | 是 | 查看率减机构基准率，单位百分点 |
| `data.contactRateDifference` | number | 是 | 接触率减机构基准率，单位百分点 |

**成功响应示例**（无数据结构示例，编号、名称为占位值）：

```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "planCount": 0,
    "skillCount": 0,
    "taskCount": 0,
    "scheduledTaskCount": 0,
    "executedTaskCount": 0,
    "pendingTaskCount": 0,
    "failedTaskCount": 0,
    "recommendedCustomerCount": 0,
    "coveredSubBranchCount": 0,
    "mostUsedSkill": null,
    "averageTaskCount": null,
    "averageRecommendedCustomerCount": null,
    "viewRate": null,
    "contactRate": null,
    "benchmarkViewRate": null,
    "benchmarkContactRate": null,
    "viewRateDifference": null,
    "contactRateDifference": null
  },
  "success": true
}
```

**规则**：所有指标按所选日期区间整体去重。机构基准保留日期及分行条件，取消支行、等级和技能条件；未选分行时为全行。

## 3.3 分支行规划覆盖

**请求**：`POST /api/v1/reports/plan-coverage`

**请求字段**

“必填”包括字段校验和业务校验；使用点号表示嵌套字段，`[]` 表示数组元素。

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `bbkId` | string | 否 | 分行编号；请求未传、null或空字符串表示全部 |
| `orgId` | string | 否 | 支行编号；请求未传、null或空字符串表示全部 |
| `level` | string | 否 | 客户经理等级，使用数据库实际值 |
| `skillIds` | `array<string>` | 否 | 技能编号数组，多选取或；未传、null或空数组表示全部 |
| `startDate` | string(date) | 是 | 排程开始日期，yyyy-MM-dd，含当日 |
| `endDate` | string(date) | 是 | 排程结束日期，yyyy-MM-dd，含当日，不得早于开始日期 |

**请求示例**

```json
{
  "bbkId": "B01",
  "orgId": "O01",
  "level": "L1",
  "skillIds": [
    "S01"
  ],
  "startDate": "2026-11-01",
  "endDate": "2026-11-12"
}
```

**响应字段**

响应表的“可空”描述字段值；嵌套字段仅在父对象非null或数组存在元素时适用。

| 字段 | 类型 | 可空 | 说明 |
| --- | --- | --- | --- |
| `code` | integer | 否 | 业务码，成功为200 |
| `message` | string | 否 | 结果说明 |
| `success` | boolean | 否 | 成功为true |
| `data` | object | 是 | 成功时为下列业务对象，失败时为null |
| `data.branches` | `array<object>` | 否 | 分行数组 |
| `data.branches[].id` | string | 是 | 编号，按编号识别对象 |
| `data.branches[].name` | string | 是 | 展示名称 |
| `data.branches[].myCount` | integer(int64) | 否 | 我的关注来源规划数 |
| `data.branches[].branchCount` | integer(int64) | 否 | 分行关注来源规划数，数据库来源为分行中台 |
| `data.branches[].presidentCount` | integer(int64) | 否 | 行长关注来源规划数 |
| `data.branches[].planCount` | integer(int64) | 否 | 规划数，按规划编号去重 |
| `data.branches[].subBranchCount` | integer(int64) | 否 | 覆盖支行数量，按机构组合去重 |
| `data.branches[].children` | `array<object>` | 否 | 下属支行数组，支行节点为空数组 |
| `data.branches[].children[].id` | string | 是 | 编号，按编号识别对象 |
| `data.branches[].children[].name` | string | 是 | 展示名称 |
| `data.branches[].children[].myCount` | integer(int64) | 否 | 我的关注来源规划数 |
| `data.branches[].children[].branchCount` | integer(int64) | 否 | 分行关注来源规划数，数据库来源为分行中台 |
| `data.branches[].children[].presidentCount` | integer(int64) | 否 | 行长关注来源规划数 |
| `data.branches[].children[].planCount` | integer(int64) | 否 | 规划数，按规划编号去重 |
| `data.branches[].children[].subBranchCount` | integer(int64) | 否 | 覆盖支行数量，按机构组合去重 |
| `data.branches[].children[].children` | `array<object>` | 否 | 下属支行数组，支行节点为空数组 |
| `data.branches[].children[].children[].id` | string | 是 | 编号，按编号识别对象 |
| `data.branches[].children[].children[].name` | string | 是 | 展示名称 |
| `data.branches[].children[].children[].myCount` | integer(int64) | 否 | 我的关注来源规划数 |
| `data.branches[].children[].children[].branchCount` | integer(int64) | 否 | 分行关注来源规划数，数据库来源为分行中台 |
| `data.branches[].children[].children[].presidentCount` | integer(int64) | 否 | 行长关注来源规划数 |
| `data.branches[].children[].children[].planCount` | integer(int64) | 否 | 规划数，按规划编号去重 |
| `data.branches[].children[].children[].subBranchCount` | integer(int64) | 否 | 覆盖支行数量，按机构组合去重 |
| `data.branches[].children[].children[].children` | `array<object>` | 否 | 下属支行数组，支行节点为空数组 |

**成功响应示例**（无数据结构示例，编号、名称为占位值）：

```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "branches": []
  },
  "success": true
}
```

**规则**：仅统计我的关注、分行中台、行长关注三类来源；分行中台返回为分行关注。分行重新按规划编号去重，不累加支行规划数。支持分行→支行两级，支行children为空。

## 3.4 规划明细

**请求**：`POST /api/v1/reports/plan-coverage/details`

**请求字段**

“必填”包括字段校验和业务校验；使用点号表示嵌套字段，`[]` 表示数组元素。

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `filters` | object | 是 | 顶部检索条件，嵌套日期必填 |
| `filters.bbkId` | string | 否 | 分行编号；请求未传、null或空字符串表示全部 |
| `filters.orgId` | string | 否 | 支行编号；请求未传、null或空字符串表示全部 |
| `filters.level` | string | 否 | 客户经理等级，使用数据库实际值 |
| `filters.skillIds` | `array<string>` | 否 | 技能编号数组，多选取或；未传、null或空数组表示全部 |
| `filters.startDate` | string(date) | 是 | 排程开始日期，yyyy-MM-dd，含当日 |
| `filters.endDate` | string(date) | 是 | 排程结束日期，yyyy-MM-dd，含当日，不得早于开始日期 |
| `scopeBbkId` | string | 是 | 点击机构的分行编号，须在顶部机构范围内 |
| `scopeOrgId` | string | 否 | 点击机构的支行编号，须在顶部范围内；省略、null或空字符串为分行下钻 |
| `source` | string | 否 | 规划来源；全部、我的关注、分行关注、行长关注，未传、null或空字符串表示全部 |

**请求示例**

```json
{
  "filters": {
    "bbkId": "B01",
    "orgId": "O01",
    "level": "L1",
    "skillIds": [
      "S01"
    ],
    "startDate": "2026-11-01",
    "endDate": "2026-11-12"
  },
  "scopeBbkId": "B01",
  "scopeOrgId": "O01",
  "source": "全部"
}
```

**响应字段**

响应表的“可空”描述字段值；嵌套字段仅在父对象非null或数组存在元素时适用。

| 字段 | 类型 | 可空 | 说明 |
| --- | --- | --- | --- |
| `code` | integer | 否 | 业务码，成功为200 |
| `message` | string | 否 | 结果说明 |
| `success` | boolean | 否 | 成功为true |
| `data` | object | 是 | 成功时为下列业务对象，失败时为null |
| `data.bbkId` | string | 是 | 分行编号 |
| `data.orgId` | string | 是 | 支行编号；规划分行下钻可为null |
| `data.name` | string | 是 | 展示名称 |
| `data.planCount` | integer(int64) | 否 | 规划数，按规划编号去重 |
| `data.recommendedCustomerCount` | integer(int64) | 否 | 推荐客户数，非空custuid在当前范围整体去重 |
| `data.taskCount` | integer(int64) | 否 | 任务数；成功推荐客户方案数，仅统计plan类型成功最新结果，排除空客户标识，按(trace_id,custuid)去重 |
| `data.scheduledTaskCount` | integer(int64) | 否 | 应执行任务数；按目标客户经理工号、规划编号、技能编号、排程日期、任务编号去重，包含未执行任务；无数据为0 |
| `data.executedTaskCount` | integer(int64) | 否 | 已执行任务数；应执行任务中存在执行记录，失败也算已执行；无数据为0 |
| `data.pendingTaskCount` | integer(int64) | 否 | 待执行任务数；应执行任务中无执行记录，不区分未来和已到期；无数据为0 |
| `data.failedTaskCount` | integer(int64) | 否 | 失败任务数；已执行任务最新记录的status不为success；无数据为0 |
| `data.skillCount` | integer(int64) | 否 | 技能数，按技能编号去重 |
| `data.myCount` | integer(int64) | 否 | 我的关注来源规划数 |
| `data.branchCount` | integer(int64) | 否 | 分行关注来源规划数，数据库来源为分行中台 |
| `data.presidentCount` | integer(int64) | 否 | 行长关注来源规划数 |
| `data.plans` | `array<object>` | 否 | 规划明细数组 |
| `data.plans[].id` | string | 是 | 编号，按编号识别对象 |
| `data.plans[].name` | string | 是 | 展示名称 |
| `data.plans[].source` | string | 是 | 规划来源名称：我的关注、分行关注、行长关注 |
| `data.plans[].ownerId` | string | 是 | 规划创建人工号（负责人） |
| `data.plans[].ownerName` | string | 是 | 规划创建人姓名 |
| `data.plans[].ownerLevel` | string | 是 | 规划创建人等级 |
| `data.plans[].periodStart` | string | 是 | 规划有效期开始值，仅展示，不用作统计日期 |
| `data.plans[].periodEnd` | string | 是 | 规划有效期结束值，仅展示，不用作统计日期 |
| `data.plans[].skills` | `array<object>` | 否 | 技能数组 |
| `data.plans[].skills[].id` | string | 是 | 编号，按编号识别对象 |
| `data.plans[].skills[].name` | string | 是 | 展示名称 |
| `data.plans[].recommendedCustomerCount` | integer(int64) | 否 | 推荐客户数，非空custuid在当前范围整体去重 |
| `data.plans[].taskCount` | integer(int64) | 否 | 任务数；成功推荐客户方案数，仅统计plan类型成功最新结果，排除空客户标识，按(trace_id,custuid)去重 |
| `data.plans[].scheduledTaskCount` | integer(int64) | 否 | 应执行任务数；按目标客户经理工号、规划编号、技能编号、排程日期、任务编号去重，包含未执行任务；无数据为0 |
| `data.plans[].executedTaskCount` | integer(int64) | 否 | 已执行任务数；应执行任务中存在执行记录，失败也算已执行；无数据为0 |
| `data.plans[].pendingTaskCount` | integer(int64) | 否 | 待执行任务数；应执行任务中无执行记录，不区分未来和已到期；无数据为0 |
| `data.plans[].failedTaskCount` | integer(int64) | 否 | 失败任务数；已执行任务最新记录的status不为success；无数据为0 |

**成功响应示例**（无数据结构示例，编号、名称为占位值）：

```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "bbkId": "B01",
    "orgId": "O01",
    "name": null,
    "planCount": 0,
    "recommendedCustomerCount": 0,
    "taskCount": 0,
    "scheduledTaskCount": 0,
    "executedTaskCount": 0,
    "pendingTaskCount": 0,
    "failedTaskCount": 0,
    "skillCount": 0,
    "myCount": 0,
    "branchCount": 0,
    "presidentCount": 0,
    "plans": []
  },
  "success": true
}
```

**规则**：scopeOrgId可省略、传null或空字符串，此时下钻分行并保留filters中的支行限制。source只过滤plans，不重算汇总和来源数量；不接受分行中台作为source参数。范围内无执行时name为null。

## 3.5 执行链路概览

**请求**：`POST /api/v1/reports/execution-overview`

**请求字段**

“必填”包括字段校验和业务校验；使用点号表示嵌套字段，`[]` 表示数组元素。

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `bbkId` | string | 否 | 分行编号；请求未传、null或空字符串表示全部 |
| `orgId` | string | 否 | 支行编号；请求未传、null或空字符串表示全部 |
| `level` | string | 否 | 客户经理等级，使用数据库实际值 |
| `skillIds` | `array<string>` | 否 | 技能编号数组，多选取或；未传、null或空数组表示全部 |
| `startDate` | string(date) | 是 | 排程开始日期，yyyy-MM-dd，含当日 |
| `endDate` | string(date) | 是 | 排程结束日期，yyyy-MM-dd，含当日，不得早于开始日期 |

**请求示例**

```json
{
  "bbkId": "B01",
  "orgId": "O01",
  "level": "L1",
  "skillIds": [
    "S01"
  ],
  "startDate": "2026-11-01",
  "endDate": "2026-11-12"
}
```

**响应字段**

响应表的“可空”描述字段值；嵌套字段仅在父对象非null或数组存在元素时适用。

| 字段 | 类型 | 可空 | 说明 |
| --- | --- | --- | --- |
| `code` | integer | 否 | 业务码，成功为200 |
| `message` | string | 否 | 结果说明 |
| `success` | boolean | 否 | 成功为true |
| `data` | object | 是 | 成功时为下列业务对象，失败时为null |
| `data.taskCount` | integer(int64) | 否 | 任务数；成功推荐客户方案数，仅统计plan类型成功最新结果，排除空客户标识，按(trace_id,custuid)去重 |
| `data.scheduledTaskCount` | integer(int64) | 否 | 应执行任务数；按目标客户经理工号、规划编号、技能编号、排程日期、任务编号去重，包含未执行任务；无数据为0 |
| `data.executedTaskCount` | integer(int64) | 否 | 已执行任务数；应执行任务中存在执行记录，失败也算已执行；无数据为0 |
| `data.pendingTaskCount` | integer(int64) | 否 | 待执行任务数；应执行任务中无执行记录，不区分未来和已到期；无数据为0 |
| `data.failedTaskCount` | integer(int64) | 否 | 失败任务数；已执行任务最新记录的status不为success；无数据为0 |
| `data.recommendedCustomerCount` | integer(int64) | 否 | 推荐客户数，非空custuid在当前范围整体去重 |
| `data.viewCustomerCount` | integer(int64) | 否 | 方案被查看客户数 |
| `data.insightCustomerCount` | integer(int64) | 否 | 点击洞察按钮的推荐客户数，button_type=insight |
| `data.callCustomerCount` | integer(int64) | 否 | 点击电访按钮的推荐客户数，button_type=phone |
| `data.contactCustomerCount` | integer(int64) | 否 | 接触客户数，沿用顶部接触口径 |
| `data.viewRate` | number | 是 | 方案被查看率，百分比数值 |
| `data.insightRate` | number | 是 | 洞察率，百分比数值 |
| `data.callRate` | number | 是 | 电访率，百分比数值 |
| `data.contactRate` | number | 是 | 接触率，百分比数值，可能超过100 |
| `data.managerCount` | integer(int64) | 否 | 执行客户经理人数，按工号去重 |
| `data.customersPerTask` | number | 是 | 推荐客户数÷任务数，保留1位小数；任务数为0时null |
| `data.averageTaskCount` | integer(int64) | 是 | 人均任务数，四舍五入为整数，无人员为null |
| `data.averageRecommendedCustomerCount` | integer(int64) | 是 | 人均推荐客户数，四舍五入为整数，无人员为null |
| `data.averageContactCustomerCount` | integer(int64) | 是 | 人均接触客户数，四舍五入为整数，无人员为null |
| `data.largestLoss` | object | 是 | 最小相邻流失率对应环节；全部分母为0时null |
| `data.largestLoss.fromStage` | string | 是 | 上一阶段，view/insight/call |
| `data.largestLoss.toStage` | string | 是 | 当前阶段，insight/call/contact |
| `data.largestLoss.customerDifference` | integer(int64) | 否 | 上一阶段客户数－当前阶段客户数，允许负值 |
| `data.largestLoss.lossRate` | number | 是 | 相邻流失率，百分比数值，允许负值 |

**成功响应示例**（无数据结构示例，编号、名称为占位值）：

```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "taskCount": 0,
    "scheduledTaskCount": 0,
    "executedTaskCount": 0,
    "pendingTaskCount": 0,
    "failedTaskCount": 0,
    "recommendedCustomerCount": 0,
    "viewCustomerCount": 0,
    "insightCustomerCount": 0,
    "callCustomerCount": 0,
    "contactCustomerCount": 0,
    "viewRate": null,
    "insightRate": null,
    "callRate": null,
    "contactRate": null,
    "managerCount": 0,
    "customersPerTask": null,
    "averageTaskCount": null,
    "averageRecommendedCustomerCount": null,
    "averageContactCustomerCount": null,
    "largestLoss": null
  },
  "success": true
}
```

**规则**：阶段依次为任务、推荐、查看、洞察、电访、接触。最大流失环节比较查看→洞察、洞察→电访、电访→接触，按(上一阶段客户数－当前阶段客户数)÷上一阶段客户数取最小值；零分母不参与，全部不可计算时largestLoss=null；并列取链路第一项，允许负值。

## 3.6 支行执行明细

**请求**：`POST /api/v1/reports/execution-branches`

**请求字段**

“必填”包括字段校验和业务校验；使用点号表示嵌套字段，`[]` 表示数组元素。

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `filters` | object | 是 | 顶部检索条件，嵌套日期必填 |
| `filters.bbkId` | string | 否 | 分行编号；请求未传、null或空字符串表示全部 |
| `filters.orgId` | string | 否 | 支行编号；请求未传、null或空字符串表示全部 |
| `filters.level` | string | 否 | 客户经理等级，使用数据库实际值 |
| `filters.skillIds` | `array<string>` | 否 | 技能编号数组，多选取或；未传、null或空数组表示全部 |
| `filters.startDate` | string(date) | 是 | 排程开始日期，yyyy-MM-dd，含当日 |
| `filters.endDate` | string(date) | 是 | 排程结束日期，yyyy-MM-dd，含当日，不得早于开始日期 |
| `sortKey` | string | 否 | 排序字段，省略、null或空字符串默认taskCount；支持skillCount、taskCount、recommendedCustomerCount、viewRate、insightRate、callRate、contactRate |
| `sortDir` | string | 否 | 排序方向asc/desc，省略、null或空字符串默认desc |

**请求示例**

```json
{
  "filters": {
    "bbkId": "B01",
    "orgId": "O01",
    "level": "L1",
    "skillIds": [
      "S01"
    ],
    "startDate": "2026-11-01",
    "endDate": "2026-11-12"
  },
  "sortKey": "taskCount",
  "sortDir": "desc"
}
```

**响应字段**

响应表的“可空”描述字段值；嵌套字段仅在父对象非null或数组存在元素时适用。

| 字段 | 类型 | 可空 | 说明 |
| --- | --- | --- | --- |
| `code` | integer | 否 | 业务码，成功为200 |
| `message` | string | 否 | 结果说明 |
| `success` | boolean | 否 | 成功为true |
| `data` | object | 是 | 成功时为下列业务对象，失败时为null |
| `data.benchmark` | object | 否 | 保留日期和分行条件重算的四项机构基准率 |
| `data.benchmark.viewRate` | number | 是 | 方案被查看率，百分比数值 |
| `data.benchmark.insightRate` | number | 是 | 洞察率，百分比数值 |
| `data.benchmark.callRate` | number | 是 | 电访率，百分比数值 |
| `data.benchmark.contactRate` | number | 是 | 接触率，百分比数值，可能超过100 |
| `data.rows` | `array<object>` | 否 | 明细数组；无数据为空数组 |
| `data.rows[].bbkId` | string | 是 | 分行编号 |
| `data.rows[].bbkName` | string | 是 | 分行名称 |
| `data.rows[].orgId` | string | 是 | 支行编号；规划分行下钻可为null |
| `data.rows[].orgName` | string | 是 | 支行名称 |
| `data.rows[].skillCount` | integer(int64) | 否 | 技能数，按技能编号去重 |
| `data.rows[].taskCount` | integer(int64) | 否 | 任务数；成功推荐客户方案数，仅统计plan类型成功最新结果，排除空客户标识，按(trace_id,custuid)去重 |
| `data.rows[].scheduledTaskCount` | integer(int64) | 否 | 应执行任务数；按目标客户经理工号、规划编号、技能编号、排程日期、任务编号去重，包含未执行任务；无数据为0 |
| `data.rows[].executedTaskCount` | integer(int64) | 否 | 已执行任务数；应执行任务中存在执行记录，失败也算已执行；无数据为0 |
| `data.rows[].pendingTaskCount` | integer(int64) | 否 | 待执行任务数；应执行任务中无执行记录，不区分未来和已到期；无数据为0 |
| `data.rows[].failedTaskCount` | integer(int64) | 否 | 失败任务数；已执行任务最新记录的status不为success；无数据为0 |
| `data.rows[].recommendedCustomerCount` | integer(int64) | 否 | 推荐客户数，非空custuid在当前范围整体去重 |
| `data.rows[].viewCustomerCount` | integer(int64) | 否 | 方案被查看客户数 |
| `data.rows[].insightCustomerCount` | integer(int64) | 否 | 点击洞察按钮的推荐客户数，button_type=insight |
| `data.rows[].callCustomerCount` | integer(int64) | 否 | 点击电访按钮的推荐客户数，button_type=phone |
| `data.rows[].contactCustomerCount` | integer(int64) | 否 | 接触客户数，沿用顶部接触口径 |
| `data.rows[].viewRate` | number | 是 | 方案被查看率，百分比数值 |
| `data.rows[].insightRate` | number | 是 | 洞察率，百分比数值 |
| `data.rows[].callRate` | number | 是 | 电访率，百分比数值 |
| `data.rows[].contactRate` | number | 是 | 接触率，百分比数值，可能超过100 |
| `data.rows[].viewRateDifference` | number | 是 | 查看率减机构基准率，单位百分点 |
| `data.rows[].insightRateDifference` | number | 是 | 洞察率减机构基准率，单位百分点 |
| `data.rows[].callRateDifference` | number | 是 | 电访率减机构基准率，单位百分点 |
| `data.rows[].contactRateDifference` | number | 是 | 接触率减机构基准率，单位百分点 |
| `data.rows[].viewRateStatus` | string | 是 | 查看率评价high/low/normal；不可比较为null |
| `data.rows[].insightRateStatus` | string | 是 | 洞察率评价high/low/normal；不可比较为null |
| `data.rows[].callRateStatus` | string | 是 | 电访率评价high/low/normal；不可比较为null |
| `data.rows[].contactRateStatus` | string | 是 | 接触率评价high/low/normal；不可比较为null |

**成功响应示例**（无数据结构示例，编号、名称为占位值）：

```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "benchmark": {
      "viewRate": null,
      "insightRate": null,
      "callRate": null,
      "contactRate": null
    },
    "rows": []
  },
  "success": true
}
```

**规则**：七个约定字段支持升降序，默认任务数倒序。null比率在两种方向中均排最后；同值按分行编号、支行编号升序。benchmark为机构整体重算率值；差值达到+3个百分点标high，达到-3标low，否则normal，使用未舍入差值判断。无法计算时差值及标识为null。

## 3.7 客户经理执行明细

**请求**：`POST /api/v1/reports/execution-branches/managers`

**请求字段**

“必填”包括字段校验和业务校验；使用点号表示嵌套字段，`[]` 表示数组元素。

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `filters` | object | 是 | 顶部检索条件，嵌套日期必填 |
| `filters.bbkId` | string | 否 | 分行编号；请求未传、null或空字符串表示全部 |
| `filters.orgId` | string | 否 | 支行编号；请求未传、null或空字符串表示全部 |
| `filters.level` | string | 否 | 客户经理等级，使用数据库实际值 |
| `filters.skillIds` | `array<string>` | 否 | 技能编号数组，多选取或；未传、null或空数组表示全部 |
| `filters.startDate` | string(date) | 是 | 排程开始日期，yyyy-MM-dd，含当日 |
| `filters.endDate` | string(date) | 是 | 排程结束日期，yyyy-MM-dd，含当日，不得早于开始日期 |
| `scopeBbkId` | string | 是 | 点击机构的分行编号，须在顶部机构范围内 |
| `scopeOrgId` | string | 是 | 点击机构的支行编号，须在顶部范围内 |

**请求示例**

```json
{
  "filters": {
    "bbkId": "B01",
    "orgId": "O01",
    "level": "L1",
    "skillIds": [
      "S01"
    ],
    "startDate": "2026-11-01",
    "endDate": "2026-11-12"
  },
  "scopeBbkId": "B01",
  "scopeOrgId": "O01"
}
```

**响应字段**

响应表的“可空”描述字段值；嵌套字段仅在父对象非null或数组存在元素时适用。

| 字段 | 类型 | 可空 | 说明 |
| --- | --- | --- | --- |
| `code` | integer | 否 | 业务码，成功为200 |
| `message` | string | 否 | 结果说明 |
| `success` | boolean | 否 | 成功为true |
| `data` | object | 是 | 成功时为下列业务对象，失败时为null |
| `data.bbkId` | string | 是 | 分行编号 |
| `data.bbkName` | string | 是 | 分行名称 |
| `data.orgId` | string | 是 | 支行编号；规划分行下钻可为null |
| `data.orgName` | string | 是 | 支行名称 |
| `data.summary` | object | 否 | 当前点击支行整体重新去重的汇总 |
| `data.summary.managerCount` | integer(int64) | 否 | 执行客户经理人数，按工号去重 |
| `data.summary.taskCount` | integer(int64) | 否 | 任务数；成功推荐客户方案数，仅统计plan类型成功最新结果，排除空客户标识，按(trace_id,custuid)去重 |
| `data.summary.scheduledTaskCount` | integer(int64) | 否 | 应执行任务数；按目标客户经理工号、规划编号、技能编号、排程日期、任务编号去重，包含未执行任务；无数据为0 |
| `data.summary.executedTaskCount` | integer(int64) | 否 | 已执行任务数；应执行任务中存在执行记录，失败也算已执行；无数据为0 |
| `data.summary.pendingTaskCount` | integer(int64) | 否 | 待执行任务数；应执行任务中无执行记录，不区分未来和已到期；无数据为0 |
| `data.summary.failedTaskCount` | integer(int64) | 否 | 失败任务数；已执行任务最新记录的status不为success；无数据为0 |
| `data.summary.recommendedCustomerCount` | integer(int64) | 否 | 推荐客户数，非空custuid在当前范围整体去重 |
| `data.summary.contactRate` | number | 是 | 接触率，百分比数值，可能超过100 |
| `data.rows` | `array<object>` | 否 | 明细数组；无数据为空数组 |
| `data.rows[].sapId` | string | 是 | 客户经理工号 |
| `data.rows[].userName` | string | 是 | 客户经理姓名 |
| `data.rows[].level` | string | 是 | 客户经理等级，使用数据库实际值 |
| `data.rows[].skills` | `array<object>` | 否 | 技能数组 |
| `data.rows[].skills[].skillId` | string | 是 | 技能编号 |
| `data.rows[].skills[].skillName` | string | 是 | 技能名称 |
| `data.rows[].taskCount` | integer(int64) | 否 | 任务数；成功推荐客户方案数，仅统计plan类型成功最新结果，排除空客户标识，按(trace_id,custuid)去重 |
| `data.rows[].scheduledTaskCount` | integer(int64) | 否 | 应执行任务数；按目标客户经理工号、规划编号、技能编号、排程日期、任务编号去重，包含未执行任务；无数据为0 |
| `data.rows[].executedTaskCount` | integer(int64) | 否 | 已执行任务数；应执行任务中存在执行记录，失败也算已执行；无数据为0 |
| `data.rows[].pendingTaskCount` | integer(int64) | 否 | 待执行任务数；应执行任务中无执行记录，不区分未来和已到期；无数据为0 |
| `data.rows[].failedTaskCount` | integer(int64) | 否 | 失败任务数；已执行任务最新记录的status不为success；无数据为0 |
| `data.rows[].recommendedCustomerCount` | integer(int64) | 否 | 推荐客户数，非空custuid在当前范围整体去重 |
| `data.rows[].viewRate` | number | 是 | 方案被查看率，百分比数值 |
| `data.rows[].insightRate` | number | 是 | 洞察率，百分比数值 |
| `data.rows[].callRate` | number | 是 | 电访率，百分比数值 |
| `data.rows[].contactRate` | number | 是 | 接触率，百分比数值，可能超过100 |

**成功响应示例**（无数据结构示例，编号、名称为占位值）：

```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "bbkId": "B01",
    "bbkName": "示例分行",
    "orgId": "O01",
    "orgName": "示例支行",
    "summary": {
      "managerCount": 0,
      "taskCount": 0,
      "scheduledTaskCount": 0,
      "executedTaskCount": 0,
      "pendingTaskCount": 0,
      "failedTaskCount": 0,
      "recommendedCustomerCount": 0,
      "contactRate": null
    },
    "rows": []
  },
  "success": true
}
```

**规则**：继承filters并限定点击支行，按任务数倒序、工号升序。客户跨人员可能重复，summary重新去重，不能求和rows得到汇总。skills仅展示筛选范围内实际参与执行的技能。无数据时返回空rows；机构记录存在时返回机构名称，不存在时名称为null。

## 3.8 技能执行明细

**请求**：`POST /api/v1/reports/execution-branches/skills`

**请求字段**

“必填”包括字段校验和业务校验；使用点号表示嵌套字段，`[]` 表示数组元素。

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `filters` | object | 是 | 顶部检索条件，嵌套日期必填 |
| `filters.bbkId` | string | 否 | 分行编号；请求未传、null或空字符串表示全部 |
| `filters.orgId` | string | 否 | 支行编号；请求未传、null或空字符串表示全部 |
| `filters.level` | string | 否 | 客户经理等级，使用数据库实际值 |
| `filters.skillIds` | `array<string>` | 否 | 技能编号数组，多选取或；未传、null或空数组表示全部 |
| `filters.startDate` | string(date) | 是 | 排程开始日期，yyyy-MM-dd，含当日 |
| `filters.endDate` | string(date) | 是 | 排程结束日期，yyyy-MM-dd，含当日，不得早于开始日期 |
| `scopeBbkId` | string | 是 | 点击机构的分行编号，须在顶部机构范围内 |
| `scopeOrgId` | string | 是 | 点击机构的支行编号，须在顶部范围内 |

**请求示例**

```json
{
  "filters": {
    "bbkId": "B01",
    "orgId": "O01",
    "level": "L1",
    "skillIds": [
      "S01"
    ],
    "startDate": "2026-11-01",
    "endDate": "2026-11-12"
  },
  "scopeBbkId": "B01",
  "scopeOrgId": "O01"
}
```

**响应字段**

响应表的“可空”描述字段值；嵌套字段仅在父对象非null或数组存在元素时适用。

| 字段 | 类型 | 可空 | 说明 |
| --- | --- | --- | --- |
| `code` | integer | 否 | 业务码，成功为200 |
| `message` | string | 否 | 结果说明 |
| `success` | boolean | 否 | 成功为true |
| `data` | object | 是 | 成功时为下列业务对象，失败时为null |
| `data.bbkId` | string | 是 | 分行编号 |
| `data.bbkName` | string | 是 | 分行名称 |
| `data.orgId` | string | 是 | 支行编号；规划分行下钻可为null |
| `data.orgName` | string | 是 | 支行名称 |
| `data.summary` | object | 否 | 当前点击支行整体重新去重的汇总 |
| `data.summary.skillCount` | integer(int64) | 否 | 技能数，按技能编号去重 |
| `data.summary.managerCount` | integer(int64) | 否 | 执行客户经理人数，按工号去重 |
| `data.summary.taskCount` | integer(int64) | 否 | 任务数；成功推荐客户方案数，仅统计plan类型成功最新结果，排除空客户标识，按(trace_id,custuid)去重 |
| `data.summary.scheduledTaskCount` | integer(int64) | 否 | 应执行任务数；按目标客户经理工号、规划编号、技能编号、排程日期、任务编号去重，包含未执行任务；无数据为0 |
| `data.summary.executedTaskCount` | integer(int64) | 否 | 已执行任务数；应执行任务中存在执行记录，失败也算已执行；无数据为0 |
| `data.summary.pendingTaskCount` | integer(int64) | 否 | 待执行任务数；应执行任务中无执行记录，不区分未来和已到期；无数据为0 |
| `data.summary.failedTaskCount` | integer(int64) | 否 | 失败任务数；已执行任务最新记录的status不为success；无数据为0 |
| `data.summary.planCount` | integer(int64) | 否 | 规划数，按规划编号去重 |
| `data.rows` | `array<object>` | 否 | 明细数组；无数据为空数组 |
| `data.rows[].skillId` | string | 是 | 技能编号 |
| `data.rows[].skillName` | string | 是 | 技能名称 |
| `data.rows[].categoryName` | string | 是 | 技能分类名称；按category_id及相同source_id关联分类表name，未配置、记录不存在或入口不一致时为null |
| `data.rows[].managerCount` | integer(int64) | 否 | 执行客户经理人数，按工号去重 |
| `data.rows[].taskCount` | integer(int64) | 否 | 任务数；成功推荐客户方案数，仅统计plan类型成功最新结果，排除空客户标识，按(trace_id,custuid)去重 |
| `data.rows[].scheduledTaskCount` | integer(int64) | 否 | 应执行任务数；按目标客户经理工号、规划编号、技能编号、排程日期、任务编号去重，包含未执行任务；无数据为0 |
| `data.rows[].executedTaskCount` | integer(int64) | 否 | 已执行任务数；应执行任务中存在执行记录，失败也算已执行；无数据为0 |
| `data.rows[].pendingTaskCount` | integer(int64) | 否 | 待执行任务数；应执行任务中无执行记录，不区分未来和已到期；无数据为0 |
| `data.rows[].failedTaskCount` | integer(int64) | 否 | 失败任务数；已执行任务最新记录的status不为success；无数据为0 |
| `data.rows[].recommendedCustomerCount` | integer(int64) | 否 | 推荐客户数，非空custuid在当前范围整体去重 |
| `data.rows[].viewRate` | number | 是 | 方案被查看率，百分比数值 |
| `data.rows[].contactRate` | number | 是 | 接触率，百分比数值，可能超过100 |
| `data.rows[].planCount` | integer(int64) | 否 | 规划数，按规划编号去重 |

**成功响应示例**（无数据结构示例，编号、名称为占位值）：

```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "bbkId": "B01",
    "bbkName": "示例分行",
    "orgId": "O01",
    "orgName": "示例支行",
    "summary": {
      "skillCount": 0,
      "managerCount": 0,
      "taskCount": 0,
      "scheduledTaskCount": 0,
      "executedTaskCount": 0,
      "pendingTaskCount": 0,
      "failedTaskCount": 0,
      "planCount": 0
    },
    "rows": []
  },
  "success": true
}
```

**规则**：继承filters并限定点击支行，按任务数倒序、技能编号升序。分类名称按技能category_id关联swe_marketplace_categories.id，并匹配source_id；未匹配为null，不过滤branch_visible。summary在支行整体去重，不能累加技能行。无数据时返回空rows；机构记录存在时返回机构名称，不存在时名称为null。

## 3.9 规划来源结构 Top N

**请求**：`POST /api/v1/reports/plan-sources`

**请求字段**

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `topN` | integer(int32) | 否 | 返回分行数，正整数；未传或null默认5，不设额外上限 |
| `bbkId` | string | 否 | 分行编号，未传、null或空字符串表示全部 |
| `orgId` | string | 否 | 支行编号，未传、null或空字符串表示全部 |
| `level` | string | 否 | 目标客户经理等级，使用数据库实际值 |
| `skillIds` | `array<string>` | 否 | 技能编号，多选取或；未传、null或空数组表示全部 |
| `startDate` | string(date) | 是 | 排程开始日期，yyyy-MM-dd，含当日 |
| `endDate` | string(date) | 是 | 排程结束日期，含当日，不得早于开始日期 |

**请求示例**

```json
{
  "startDate": "2026-11-01",
  "endDate": "2026-11-12",
  "skillIds": ["S01", "S02"],
  "topN": 3
}
```

**响应字段**

响应表的“可空”描述字段值；嵌套字段仅在父对象非null或数组存在元素时适用。

| 字段 | 类型 | 可空 | 说明 |
| --- | --- | --- | --- |
| `code` | integer | 否 | 业务码，成功为200 |
| `message` | string | 否 | 结果说明 |
| `success` | boolean | 否 | 成功为true |
| `data` | object | 是 | 成功时为下列业务对象，失败时为null |
| `data.branches` | `array<object>` | 否 | 已排序的分行数组，最多topN项，无数据为[] |
| `data.branches[].id` | string | 否 | 分行编号 |
| `data.branches[].name` | string | 是 | 分行名称 |
| `data.branches[].planCount` | integer(int64) | 否 | 当前分行按规划编号去重的规划总数 |
| `data.branches[].myCount` | integer(int64) | 否 | 我的关注规划数 |
| `data.branches[].branchCount` | integer(int64) | 否 | 分行关注规划数，数据库来源为“分行中台” |
| `data.branches[].presidentCount` | integer(int64) | 否 | 行长关注规划数 |

**成功响应示例**（示意数据，不足topN项时返回实际数量）：

```json
{
  "code": 200,
  "message": "成功",
  "success": true,
  "data": {
    "branches": [
      {
        "id": "B01",
        "name": "示例分行",
        "planCount": 7,
        "myCount": 3,
        "branchCount": 2,
        "presidentCount": 2
      }
    ]
  }
}
```

**规则**

- 先应用全部检索条件，再按分行规划总数降序取前topN项；总数相同按分行编号升序，不扩展并列名额。
- 复用规划覆盖基础数据范围，仅纳入三类指定来源；空值和其他来源排除。三类来源数量之和等于分行规划总数。
- 同一规划跨日期、人员、技能或支行，在同一分行内只计1条，不累加支行数量；不同分行各自去重，不能将Top N数量相加视为全行去重总数。
- 选择支行后，只统计该支行范围内的数据，仍返回所属分行编号和名称，不扩展到整个分行。
- 统计所选排程日期内参与执行的规划，有执行但无推荐结果仍计入；不额外按当前发布状态或有效期过滤。
- 不返回零规划分行，不补足topN项；无数据成功返回 `data={"branches":[]}`。
- 响应不含支行明细、来源占比、全行总计或规划洞察。页面可用来源数量除以该分行规划总数计算占比。
- topN小于1、日期缺失、格式错误或开始日晚于结束日时，HTTP为200、code为10000、success为false、data为null；机构组合不匹配按当前实现正常查询并返回空数组，不返回归属错误。

验证记录见 [规划来源结构接口验证记录](report-plan-source-validation.md)。记录对应当时服务版本，当前接口契约以本文及实现为准。

## 3.10 执行走势

**请求**：`POST /api/v1/reports/execution-trend`

**请求字段**

“必填”包括字段校验和业务校验；使用点号表示嵌套字段，`[]` 表示数组元素。

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `bbkId` | string | 否 | 分行编号；请求未传、null或空字符串表示全部 |
| `orgId` | string | 否 | 支行编号；请求未传、null或空字符串表示全部 |
| `level` | string | 否 | 客户经理等级，使用数据库实际值 |
| `skillIds` | `array<string>` | 否 | 技能编号数组，多选取或；未传、null或空数组表示全部 |
| `startDate` | string(date) | 是 | 排程开始日期，yyyy-MM-dd，含当日 |
| `endDate` | string(date) | 是 | 排程结束日期，yyyy-MM-dd，含当日，不得早于开始日期 |

**请求示例**

```json
{
  "bbkId": "B01",
  "orgId": "O01",
  "level": "L1",
  "skillIds": [
    "S01"
  ],
  "startDate": "2026-11-01",
  "endDate": "2026-11-01"
}
```

**响应字段**

响应表的“可空”描述字段值；嵌套字段仅在父对象非null或数组存在元素时适用。

| 字段 | 类型 | 可空 | 说明 |
| --- | --- | --- | --- |
| `code` | integer | 否 | 业务码，成功为200 |
| `message` | string | 否 | 结果说明 |
| `success` | boolean | 否 | 成功为true |
| `data` | object | 是 | 成功时为下列业务对象，失败时为null |
| `data.dailyPoints` | `array<object>` | 否 | 按日期升序排列，完整覆盖所选区间；无数据日期仍返回一项 |
| `data.dailyPoints[].date` | string(date) | 否 | 排程日期，yyyy-MM-dd |
| `data.dailyPoints[].taskCount` | integer(int64) | 否 | 当日任务数；成功推荐客户方案数，仅统计plan类型成功最新结果，排除空客户标识，按(trace_id,custuid)去重 |
| `data.dailyPoints[].scheduledTaskCount` | integer(int64) | 否 | 应执行任务数；按目标客户经理工号、规划编号、技能编号、排程日期、任务编号去重，包含未执行任务；无数据为0 |
| `data.dailyPoints[].executedTaskCount` | integer(int64) | 否 | 已执行任务数；应执行任务中存在执行记录，失败也算已执行；无数据为0 |
| `data.dailyPoints[].pendingTaskCount` | integer(int64) | 否 | 待执行任务数；应执行任务中无执行记录，不区分未来和已到期；无数据为0 |
| `data.dailyPoints[].failedTaskCount` | integer(int64) | 否 | 失败任务数；已执行任务最新记录的status不为success；无数据为0 |
| `data.dailyPoints[].recommendedCustomerCount` | integer(int64) | 否 | 当日推荐客户数，非空custuid去重 |
| `data.dailyPoints[].viewCustomerCount` | integer(int64) | 否 | 当日方案被查看客户数，is_read=1执行对应的推荐客户去重 |
| `data.dailyPoints[].insightCustomerCount` | integer(int64) | 否 | 当日洞察点击客户数，button_click且button_type=insight |
| `data.dailyPoints[].callCustomerCount` | integer(int64) | 否 | 当日电访点击客户数，button_click且button_type=phone |
| `data.dailyPoints[].contactCustomerCount` | integer(int64) | 否 | 当日接触客户数，关联接触明细cust_uid去重，NULL不计、空字符串计一个值 |
| `data.dailyPoints[].viewRate` | number | 是 | 当日方案被查看率；查看客户数÷推荐客户数×100，分母为0时为null |
| `data.dailyPoints[].insightRate` | number | 是 | 当日洞察率；洞察客户数÷推荐客户数×100，分母为0时为null |
| `data.dailyPoints[].callRate` | number | 是 | 当日电访率；电访客户数÷推荐客户数×100，分母为0时为null |
| `data.dailyPoints[].contactRate` | number | 是 | 当日接触率；接触客户数÷推荐客户数×100，分母为0时为null，可超过100 |

**成功响应示例**（示意数据，编号及名称为占位值）：

```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "dailyPoints": [
      {
        "date": "2026-11-01",
        "taskCount": 10,
        "scheduledTaskCount": 10,
        "executedTaskCount": 10,
        "pendingTaskCount": 0,
        "failedTaskCount": 0,
        "recommendedCustomerCount": 10,
        "viewCustomerCount": 8,
        "insightCustomerCount": 6,
        "callCustomerCount": 4,
        "contactCustomerCount": 2,
        "viewRate": 80,
        "insightRate": 60,
        "callRate": 40,
        "contactRate": 20
      }
    ]
  },
  "success": true
}
```

**规则**

- 按所选startDate至endDate逐日统计，包含首尾；不固定近7天。单日返回一个数据点，连续12日返回12个数据点。
- 沿用执行链路的数据关联与每天最新执行规则，洞察和电访点击须匹配推荐客户及执行当天；不要求先查看或先洞察，不代表接通。
- 同一客户关联多条任务，至少一次符合条件的查看即计入当日查看客户；四项率均以当天推荐客户数为分母，展示值四舍五入保留两位小数。
- 每日独立去重；区间累计需在整个区间重新去重，不能相加每日客户数或平均每日率替代累计指标。
- 无数据日期数量为0、四项率为null；接触率不截断至100%。
- 日期缺失、格式错误或倒置返回10000；机构无匹配数据正常返回逐日零值与null率。

## 3.11 技能参与度 Top N

**请求**：`POST /api/v1/reports/skill-participation`

**请求字段**

“必填”包括字段校验和业务校验；使用点号表示嵌套字段，`[]` 表示数组元素。

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `filters` | object | 是 | 顶部检索条件，不可null |
| `filters.bbkId` | string | 否 | 分行编号；请求未传、null或空字符串表示全部 |
| `filters.orgId` | string | 否 | 支行编号；请求未传、null或空字符串表示全部 |
| `filters.level` | string | 否 | 客户经理等级，使用数据库实际值 |
| `filters.skillIds` | `array<string>` | 否 | 技能编号数组，多选取或；未传、null或空数组表示全部 |
| `filters.startDate` | string(date) | 是 | 排程开始日期，yyyy-MM-dd，含当日 |
| `filters.endDate` | string(date) | 是 | 排程结束日期，yyyy-MM-dd，含当日，不得早于开始日期 |
| `topN` | integer(int32) | 否 | 返回技能数；正整数，未传或null默认5，不设额外业务上限 |
| `sortKey` | string | 否 | participantCount（参与人数）、participationRate（参与率）、assignedManagerCount（应执行人数）；未传或null默认participantCount，空字符串不合法 |

**请求示例**

```json
{
  "filters": {
    "bbkId": "B01",
    "orgId": "O01",
    "level": "L1",
    "skillIds": [
      "S01"
    ],
    "startDate": "2026-11-01",
    "endDate": "2026-11-12"
  },
  "topN": 5,
  "sortKey": "participantCount"
}
```

**响应字段**

响应表的“可空”描述字段值；嵌套字段仅在父对象非null或数组存在元素时适用。

| 字段 | 类型 | 可空 | 说明 |
| --- | --- | --- | --- |
| `code` | integer | 否 | 业务码，成功为200 |
| `message` | string | 否 | 结果说明 |
| `success` | boolean | 否 | 成功为true |
| `data` | object | 是 | 成功时为下列业务对象，失败时为null |
| `data.rows` | `array<object>` | 否 | 已排序的技能列表，最多topN项，无数据为[] |
| `data.rows[].skillId` | string | 否 | 技能编号，技能内按sap_id去重人员 |
| `data.rows[].skillName` | string | 是 | 技能名称，缺失时保留技能编号及统计数据 |
| `data.rows[].participantCount` | integer(int64) | 否 | 参与人数；应执行人员中，对应技能任务所选执行至少一次is_read=1的人数 |
| `data.rows[].assignedManagerCount` | integer(int64) | 否 | 应执行人数；区间内存在对应技能任务及排程的人员数，包含未执行和无成功结果人员 |
| `data.rows[].participationRate` | number | 否 | 参与率＝参与人数÷应执行人数×100，保留两位小数；零分母不入榜 |

**成功响应示例**（示意数据，编号及名称为占位值）：

```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "rows": [
      {
        "skillId": "S01",
        "skillName": "示例技能",
        "participantCount": 1,
        "assignedManagerCount": 3,
        "participationRate": 33.33
      }
    ]
  },
  "success": true
}
```

**规则**

- 分母使用符合检索条件的人员—技能任务关系及区间排程，不要求存在执行或推荐结果；分子沿用每天最新执行选择规则，以任务执行is_read=1归属目标人员，不另核实实际操作者身份。
- 同一人员在同一技能内跨日期、任务只计一次；跨技能可分别参与，不将人员全部任务重复归入每个关联技能。
- 按sortKey选定指标降序排列；参与率使用未舍入值比较，同值按技能编号升序，先排序再取前topN项。不补足名额。
- topN未传或null默认5；超过技能数返回实际条数。分母为0不入榜，有分母无人查看返回0%参与率。
- 缺失或null的filters、日期缺失/格式错误/倒置、topN非正整数或超出int32范围、非法sortKey均返回10000；JSON小数topN不静默截断。

## 3.12 需关注支行

**请求**：`POST /api/v1/reports/attention-branches`

**请求字段**

“必填”包括字段校验和业务校验；使用点号表示嵌套字段，`[]` 表示数组元素。

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `bbkId` | string | 否 | 分行编号；请求未传、null或空字符串表示全部 |
| `orgId` | string | 否 | 支行编号；请求未传、null或空字符串表示全部 |
| `level` | string | 否 | 客户经理等级，使用数据库实际值 |
| `skillIds` | `array<string>` | 否 | 技能编号数组，多选取或；未传、null或空数组表示全部 |
| `startDate` | string(date) | 是 | 排程开始日期，yyyy-MM-dd，含当日 |
| `endDate` | string(date) | 是 | 排程结束日期，yyyy-MM-dd，含当日，不得早于开始日期 |

**请求示例**

```json
{
  "bbkId": "B01",
  "orgId": "O01",
  "level": "L1",
  "skillIds": [
    "S01"
  ],
  "startDate": "2026-11-01",
  "endDate": "2026-11-12"
}
```

**响应字段**

响应表的“可空”描述字段值；嵌套字段仅在父对象非null或数组存在元素时适用。

| 字段 | 类型 | 可空 | 说明 |
| --- | --- | --- | --- |
| `code` | integer | 否 | 业务码，成功为200 |
| `message` | string | 否 | 结果说明 |
| `success` | boolean | 否 | 成功为true |
| `data` | object | 是 | 成功时为下列业务对象，失败时为null |
| `data.totalCount` | integer(int64) | 否 | 完整命中支行数，在截取前4条之前计算 |
| `data.rows` | `array<object>` | 否 | 已排序的关注支行，最多4条，无命中时为[] |
| `data.rows[].bbkId` | string | 否 | 所属分行编号，与orgId组合标识支行 |
| `data.rows[].bbkName` | string | 是 | 所属分行名称 |
| `data.rows[].orgId` | string | 否 | 支行编号，不同分行相同编号不合并 |
| `data.rows[].orgName` | string | 是 | 支行名称，缺失不剔除统计数据 |
| `data.rows[].diagnosis` | string | 否 | 诊断类型：usageLow（使用不足）、contactLow（接触不足）、bothLow（使用与接触均不足） |
| `data.rows[].recommendedCustomerCount` | integer(int64) | 否 | 支行范围推荐客户数，非空custuid整体去重 |
| `data.rows[].viewCustomerCount` | integer(int64) | 否 | 支行范围方案被查看客户数，is_read=1执行对应推荐客户整体去重 |
| `data.rows[].contactCustomerCount` | integer(int64) | 否 | 支行范围接触客户数，沿用点击关联接触明细cust_uid整体去重 |
| `data.rows[].viewRate` | number | 是 | 支行查看客户数÷推荐客户数×100，分母为0时为null |
| `data.rows[].contactRate` | number | 是 | 支行接触客户数÷推荐客户数×100，可超过100，分母为0时为null |
| `data.rows[].benchmarkViewRate` | number | 是 | 机构整体查看基准率；保留日期和分行条件，取消支行、等级、技能条件，分母为0时为null |
| `data.rows[].benchmarkContactRate` | number | 是 | 机构整体接触基准率，与查看基准使用相同范围，分母为0时为null |
| `data.rows[].viewRateDifference` | number | 是 | 支行查看率减机构查看基准率，单位百分点；任一率为null时为null |
| `data.rows[].contactRateDifference` | number | 是 | 支行接触率减机构接触基准率，单位百分点；任一率为null时为null |

**成功响应示例**（示意数据，编号及名称为占位值）：

```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "totalCount": 1,
    "rows": [
      {
        "bbkId": "B01",
        "bbkName": "示例分行",
        "orgId": "O01",
        "orgName": "示例支行",
        "diagnosis": "bothLow",
        "recommendedCustomerCount": 10,
        "viewCustomerCount": 6,
        "contactCustomerCount": 1,
        "viewRate": 60,
        "contactRate": 10,
        "benchmarkViewRate": 80,
        "benchmarkContactRate": 20,
        "viewRateDifference": -20,
        "contactRateDifference": -10
      }
    ]
  },
  "success": true
}
```

**规则**

- 对所选区间的支行客户整体去重，不能累加人员或技能行得到支行汇总；分行编号与支行编号共同标识机构。
- 基准保留日期和分行条件，取消支行、等级、技能条件，重新计算整体去重客户率，不平均支行率；未选分行时基准为全行。
- 按未舍入值严格比较：仅查看率低于基准为usageLow，仅接触率低于基准为contactLow，两项均低为bothLow。等于基准不算低；本接口不使用±3个百分点的表格颜色阈值。
- 零推荐支行不入榜；任一支行率或基准无法计算，该项不参与判断；两项均无法比较不入榜。
- contactLow和bothLow优先，按接触率升序；usageLow随后，按查看率升序。同一组率相同时按推荐客户数降序、分行编号升序、支行编号升序。率排序使用未舍入值。
- 先统计全部命中支行数再截取前4条。例如命中7家时totalCount=7、rows最多4条；不提供分页或可配置条数。无命中时totalCount=0、rows=[]。
- 百分比与差值四舍五入保留两位小数，JSON数值不保证尾随零；差值为支行率减基准率，负值表示落后，排序中的落后程度则为基准率减支行率。
- 查看与接触为并列诊断，不证明动作先后关系或因果。下钻复用客户经理执行明细接口，传filters及scopeBbkId/scopeOrgId。
- 日期缺失、格式错误或倒置返回10000；机构组合不匹配正常查询，返回totalCount=0、rows=[]，不另增归属错误。

## 4. 执行分析接口错误与验证说明

三个执行分析接口沿用第2节统一响应。参数校验失败HTTP为200、code为10000、success为false、data为null；未预期异常使用99999。message说明具体原因，客户端不按文案判断错误类型。不新增鉴权要求或错误码。

**参数错误响应示例**（开始日期晚于结束日期）：

```json
{
  "code": 10000,
  "message": "开始日期不能晚于结束日期",
  "data": null,
  "success": false
}
```

执行分析接口实际响应及数据库独立计算核对见 [执行分析接口验证记录](report-execution-analysis-validation.md)，153项通过；示例不作为运行测试结果。本次不进行原HTML接口联调。

## 5. 调用示例

```bash
curl -X POST 'http://localhost:8080/api/v1/reports/execution-branches/skills' \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/json' \
  -d '{"filters":{"startDate":"2026-11-01","endDate":"2026-11-12","bbkId":"B01"},"scopeBbkId":"B01","scopeOrgId":"O01"}'
```

上例地址与机构编号需替换为目标环境实际值。技能明细有数据时，行示例如下（此处仅为data.rows中的一行）：

```json
{
  "skillId": "S01",
  "skillName": "示例技能",
  "categoryName": "基金经营",
  "managerCount": 2,
  "taskCount": 10,
  "scheduledTaskCount": 10,
  "executedTaskCount": 10,
  "pendingTaskCount": 0,
  "failedTaskCount": 0,
  "recommendedCustomerCount": 8,
  "viewRate": 75.0,
  "contactRate": 50.0,
  "planCount": 1
}
```

## 2026-11-12 任务指标及执行链路口径更新

适用接口：POST /api/v1/reports/summary、/execution-trend、/execution-overview、/execution-branches、/execution-branches/managers、/execution-branches/skills、/plan-coverage/details。原taskCount字段名保留，表示成功推荐客户方案数；原返回taskCount的指标对象、汇总和行对象均追加上述四个非空int64字段，无数据为0。请求参数、校验、错误码不变，不新增排序键；人均任务数、户/任务及任务数排序继续使用taskCount。

应执行任务数＝已执行任务数＋待执行任务数，失败任务数≤已执行任务数。任务最新执行以actual_time确定；status仅success为成功，其余状态（error、cancelled、timeout、skipped）均失败，不使用async_status。成功推荐客户方案数仅筛选result_type=plan、is_latest_success=1、status=SUC且客户标识非空，按trace_id和custuid去重，与执行成功任务数独立。

按任务和排程日期选定最新执行后，链路回查仅取相同任务编号、相同排程日期且actual_time不早于规划创建时间的记录。技能参与度和需关注支行的响应字段不变，但查看状态、客户行为及比率受此范围修正影响。filters、plan-coverage、plan-sources保持原响应及统计范围。

未执行任务也出现在任务指标接口的每日点和对应下钻行中，原规划数、技能数、客户经理数等执行维度统计仍仅取有执行记录的关系；未执行任务不生成推荐、查看或接触。任务指标按各层实际范围重新去重，不能累加子维度代替汇总。

示例：某任务当日没有执行记录，taskCount=0、scheduledTaskCount=1、executedTaskCount=0、pendingTaskCount=1、failedTaskCount=0；最新执行为error时，后四项依次为1、1、0、1。最新执行为success且生成两个不同客户的成功方案时，taskCount=2，后四项依次为1、1、0、0。
