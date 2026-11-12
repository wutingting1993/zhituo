# 智拓报表接口文档

> 版本：v1 · 更新日期：2026-11-12 · 依据：当前 Controller、DTO 和应用服务实现。

## 1. 通用约定

- 基础路径：`/api/v1/reports`，部署地址由环境提供。
- 协议：HTTP/HTTPS；所有接口使用 `POST`，请求和响应均为 UTF-8 JSON。
- 请求头：`Content-Type: application/json`；建议 `Accept: application/json`。
- 所有接口必须提供 JSON body，检索全部候选项时传 `{}`；不从 URL 查询参数获取检索条件。
- 当前代码未声明认证头或认证机制，本接口文档不新增认证要求。
- 接口均只读，不提供分页；数组无数据返回 `[]`，数量无数据返回 `0`。
- 必填日期及必填下钻机构不因空字符串规则变为可选；`skillIds` 不使用字符串 `""` 代替数组，不新增数组元素过滤。非空字符串保持原样，不自动去除空格。
- 日期格式 `yyyy-MM-dd`，指排程 execute_day，含开始日和结束日，开始日不得晚于结束日。
- 分行、支行、等级单选，技能多选取或；可选字符串字段未传、null或空字符串 `""` 均按未传处理；技能使用数组，`[]` 表示未传，仍兼容省略或null。编号用于筛选和去重，名称仅展示。
- 查看率、洞察率、电访率、接触率均以推荐客户数为分母；例如 `62.5` 表示 `62.5%`，分母为0时为null，展示为“—”。
- 百分比、百分点差值保留两位小数，人均值四舍五入为整数，户/任务保留一位小数。JSON数值不保证保留尾随零。接触率可能超过100%，流失率可以为负。
- null与字段缺失含义不同：响应中的可空字段按实际序列化结果显式返回null。机构名称、人员名称和等级等来自可空数据库字段，可能为null。
- 各层级独立去重，不以每日、人员、技能或下级机构数值直接累加替代汇总。

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
| `data.subBranches` | `array<object>` | 否 | 支行候选项数组 |
| `data.subBranches[].id` | string | 是 | 编号，按编号识别对象 |
| `data.subBranches[].name` | string | 是 | 展示名称 |
| `data.skills` | `array<object>` | 否 | 技能数组 |
| `data.skills[].id` | string | 是 | 编号，按编号识别对象 |
| `data.skills[].name` | string | 是 | 展示名称 |
| `data.levels` | `array<string>` | 否 | 客户经理等级字符串数组 |

**成功响应示例**（无数据结构示例，编号、名称为占位值）：

```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "branches": [],
    "subBranches": [],
    "skills": [],
    "levels": []
  },
  "success": true
}
```

**规则**：分行、支行、技能候选项相互联动，查询某一类候选项时移除该类自身条件；日期不影响候选项。分支行不匹配时返回参数校验失败。

## 3.2 顶部统计指标

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
| `data.taskCount` | integer(int64) | 否 | 任务数；名单任务按trace_id、方案任务按(trace_id,custuid)去重后相加 |
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
| `scopeOrgId` | string | 否 | 点击机构的支行编号，须与分行匹配且在顶部范围内；省略、null或空字符串为分行下钻 |
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
| `data.taskCount` | integer(int64) | 否 | 任务数；名单任务按trace_id、方案任务按(trace_id,custuid)去重后相加 |
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
| `data.plans[].taskCount` | integer(int64) | 否 | 任务数；名单任务按trace_id、方案任务按(trace_id,custuid)去重后相加 |

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
| `data.taskCount` | integer(int64) | 否 | 任务数；名单任务按trace_id、方案任务按(trace_id,custuid)去重后相加 |
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
| `data.rows[].taskCount` | integer(int64) | 否 | 任务数；名单任务按trace_id、方案任务按(trace_id,custuid)去重后相加 |
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
| `scopeOrgId` | string | 是 | 点击机构的支行编号，须与分行匹配且在顶部范围内 |

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
| `data.summary.taskCount` | integer(int64) | 否 | 任务数；名单任务按trace_id、方案任务按(trace_id,custuid)去重后相加 |
| `data.summary.recommendedCustomerCount` | integer(int64) | 否 | 推荐客户数，非空custuid在当前范围整体去重 |
| `data.summary.contactRate` | number | 是 | 接触率，百分比数值，可能超过100 |
| `data.rows` | `array<object>` | 否 | 明细数组；无数据为空数组 |
| `data.rows[].sapId` | string | 是 | 客户经理工号 |
| `data.rows[].userName` | string | 是 | 客户经理姓名 |
| `data.rows[].level` | string | 是 | 客户经理等级，使用数据库实际值 |
| `data.rows[].skills` | `array<object>` | 否 | 技能数组 |
| `data.rows[].skills[].skillId` | string | 是 | 技能编号 |
| `data.rows[].skills[].skillName` | string | 是 | 技能名称 |
| `data.rows[].taskCount` | integer(int64) | 否 | 任务数；名单任务按trace_id、方案任务按(trace_id,custuid)去重后相加 |
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
      "recommendedCustomerCount": 0,
      "contactRate": null
    },
    "rows": []
  },
  "success": true
}
```

**规则**：继承filters并限定点击支行，按任务数倒序、工号升序。客户跨人员可能重复，summary重新去重，不能求和rows得到汇总。skills仅展示筛选范围内实际参与执行的技能。无数据时仍返回合法机构名称及空rows。

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
| `scopeOrgId` | string | 是 | 点击机构的支行编号，须与分行匹配且在顶部范围内 |

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
| `data.summary.taskCount` | integer(int64) | 否 | 任务数；名单任务按trace_id、方案任务按(trace_id,custuid)去重后相加 |
| `data.summary.planCount` | integer(int64) | 否 | 规划数，按规划编号去重 |
| `data.rows` | `array<object>` | 否 | 明细数组；无数据为空数组 |
| `data.rows[].skillId` | string | 是 | 技能编号 |
| `data.rows[].skillName` | string | 是 | 技能名称 |
| `data.rows[].categoryName` | string | 是 | 技能分类名称；按category_id及相同source_id关联分类表name，未配置、记录不存在或入口不一致时为null |
| `data.rows[].managerCount` | integer(int64) | 否 | 执行客户经理人数，按工号去重 |
| `data.rows[].taskCount` | integer(int64) | 否 | 任务数；名单任务按trace_id、方案任务按(trace_id,custuid)去重后相加 |
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
      "planCount": 0
    },
    "rows": []
  },
  "success": true
}
```

**规则**：继承filters并限定点击支行，按任务数倒序、技能编号升序。分类名称按技能category_id关联swe_marketplace_categories.id，并匹配source_id；未匹配为null，不过滤branch_visible。summary在支行整体去重，不能累加技能行。无数据仍返回合法机构名称及空rows。

## 4. 调用示例

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
  "recommendedCustomerCount": 8,
  "viewRate": 75.0,
  "contactRate": 50.0,
  "planCount": 1
}
```
