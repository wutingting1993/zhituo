# 智拓报表接口文档

> 版本：v1 · 更新日期：2026-11-12 · 依据：当前 Controller、DTO 和应用服务实现。

## 1. 通用约定

- 基础路径：`/api/v1/reports`，部署地址由环境提供。
- 协议：HTTP/HTTPS；所有接口使用 `POST`，请求和响应均为 UTF-8 JSON。
- 请求头：`Content-Type: application/json`；建议 `Accept: application/json`。
- 所有接口必须提供 JSON body，检索全部候选项时传 `{}`；不从 URL 查询参数获取检索条件。
- 当前代码未声明认证头或认证机制，本接口文档不新增认证要求。
- 接口均只读，不提供分页；数组无数据返回 `[]`，数量无数据返回 `0`。
- 日期格式 `yyyy-MM-dd`，指排程 execute_day，含开始日和结束日，开始日不得晚于结束日。
- 分行、支行、等级单选，技能多选取或；未传或null表示全部，技能空数组也表示全部。编号用于筛选和去重，名称仅展示。
- 查看率、洞察率、电访率、接触率均以推荐客户数为分母；例如 `62.5` 表示 `62.5%`，分母为0时为null，展示为“—”。
- 百分比、百分点差值保留两位小数，人均值四舍五入为整数，户/任务保留一位小数。JSON数值不保证保留尾随零。接触率可能超过100%，流失率可以为负。
- null与字段缺失含义不同：响应中的可空字段按实际序列化结果显式返回null。机构名称、人员名称和等级等来自可空数据库字段，可能为null。
- 各层级独立去重，不以每日、人员、技能或下级机构数值直接累加替代汇总。

## 2. 统一响应与错误码

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| code | integer | 业务码；200成功 |
| message | string | 可展示的结果说明 |
| success | boolean | 成功true，失败false |
| data | object或null | 成功数据对象；失败为null |

应用可控制的正常与异常 JSON 响应 HTTP 状态均为200；客户端应判断code和success。网关、容器等应用外部错误不保证该结构。

```json
{
  "code": 10000,
  "message": "开始日期不能晚于结束日期",
  "data": null,
  "success": false
}
```

| 业务码 | 含义 |
| --- | --- |
| 200 | 成功 |
| 10000 | 缺少body、日期缺失/格式错误/倒置、分支行不匹配、下钻越界、来源或排序枚举非法等参数错误 |
| 10001 | 业务处理失败 |
| 10002 | 请求资源不存在 |
| 10003 | 不支持的请求方法 |
| 10004 | 不支持的请求内容类型 |
| 10005 | 上传文件超过大小限制（通用错误码，本组接口不接收文件） |
| 99999 | 未知错误，处理失败，请稍后重试 |

message可能包含具体校验原因，不应按错误文案编写客户端判断逻辑。

## 3. 接口目录

| 名称 | 方法 | 路径 | 请求模型 | data模型 |
| --- | --- | --- | --- | --- |
| 检索选项 | POST | `/api/v1/reports/filters` | [ReportFilterRequest](#reportfilterrequest) | [ReportFiltersResponse](#reportfiltersresponse) |
| 顶部统计指标 | POST | `/api/v1/reports/summary` | [ReportSummaryRequest](#reportsummaryrequest) | [ReportSummaryResponse](#reportsummaryresponse) |
| 分支行规划覆盖 | POST | `/api/v1/reports/plan-coverage` | [ReportSummaryRequest](#reportsummaryrequest) | [PlanCoverageResponse](#plancoverageresponse) |
| 规划明细 | POST | `/api/v1/reports/plan-coverage/details` | [PlanDetailsRequest](#plandetailsrequest) | [PlanDetailsResponse](#plandetailsresponse) |
| 执行链路概览 | POST | `/api/v1/reports/execution-overview` | [ReportSummaryRequest](#reportsummaryrequest) | [ExecutionOverviewResponse](#executionoverviewresponse) |
| 支行执行明细 | POST | `/api/v1/reports/execution-branches` | [ExecutionBranchesRequest](#executionbranchesrequest) | [ExecutionBranchesResponse](#executionbranchesresponse) |
| 客户经理执行明细 | POST | `/api/v1/reports/execution-branches/managers` | [ExecutionDetailsRequest](#executiondetailsrequest) | [ExecutionManagerDetailsResponse](#executionmanagerdetailsresponse) |
| 技能执行明细 | POST | `/api/v1/reports/execution-branches/skills` | [ExecutionDetailsRequest](#executiondetailsrequest) | [ExecutionSkillDetailsResponse](#executionskilldetailsresponse) |

## 4.1 检索选项

**请求**：`POST /api/v1/reports/filters`

**请求体**：[ReportFilterRequest](#reportfilterrequest)。必填项见字段表。

```json
{
  "bbkId": "B01",
  "orgId": "O01",
  "skillIds": [
    "S01"
  ]
}
```

**成功响应**：`data` 为 [ReportFiltersResponse](#reportfiltersresponse)。下例为无执行数据的结构示例，编号、名称为占位值；明细元素见后续字段表。

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

**规则**：分行、支行、技能候选项相互联动，查询某一类候选项时移除该类自身条件；日期不影响候选项。分支行不匹配返回10000。

## 4.2 顶部统计指标

**请求**：`POST /api/v1/reports/summary`

**请求体**：[ReportSummaryRequest](#reportsummaryrequest)。必填项见字段表。

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

**成功响应**：`data` 为 [ReportSummaryResponse](#reportsummaryresponse)。下例为无执行数据的结构示例，编号、名称为占位值；明细元素见后续字段表。

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

## 4.3 分支行规划覆盖

**请求**：`POST /api/v1/reports/plan-coverage`

**请求体**：[ReportSummaryRequest](#reportsummaryrequest)。必填项见字段表。

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

**成功响应**：`data` 为 [PlanCoverageResponse](#plancoverageresponse)。下例为无执行数据的结构示例，编号、名称为占位值；明细元素见后续字段表。

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

## 4.4 规划明细

**请求**：`POST /api/v1/reports/plan-coverage/details`

**请求体**：[PlanDetailsRequest](#plandetailsrequest)。必填项见字段表。

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

**成功响应**：`data` 为 [PlanDetailsResponse](#plandetailsresponse)。下例为无执行数据的结构示例，编号、名称为占位值；明细元素见后续字段表。

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

**规则**：scopeOrgId可省略，此时下钻分行并保留filters中的支行限制。source只过滤plans，不重算汇总和来源数量；不接受分行中台作为source参数。范围内无执行时name为null。

## 4.5 执行链路概览

**请求**：`POST /api/v1/reports/execution-overview`

**请求体**：[ReportSummaryRequest](#reportsummaryrequest)。必填项见字段表。

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

**成功响应**：`data` 为 [ExecutionOverviewResponse](#executionoverviewresponse)。下例为无执行数据的结构示例，编号、名称为占位值；明细元素见后续字段表。

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

## 4.6 支行执行明细

**请求**：`POST /api/v1/reports/execution-branches`

**请求体**：[ExecutionBranchesRequest](#executionbranchesrequest)。必填项见字段表。

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

**成功响应**：`data` 为 [ExecutionBranchesResponse](#executionbranchesresponse)。下例为无执行数据的结构示例，编号、名称为占位值；明细元素见后续字段表。

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

## 4.7 客户经理执行明细

**请求**：`POST /api/v1/reports/execution-branches/managers`

**请求体**：[ExecutionDetailsRequest](#executiondetailsrequest)。必填项见字段表。

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

**成功响应**：`data` 为 [ExecutionManagerDetailsResponse](#executionmanagerdetailsresponse)。下例为无执行数据的结构示例，编号、名称为占位值；明细元素见后续字段表。

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

## 4.8 技能执行明细

**请求**：`POST /api/v1/reports/execution-branches/skills`

**请求体**：[ExecutionDetailsRequest](#executiondetailsrequest)。必填项见字段表。

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

**成功响应**：`data` 为 [ExecutionSkillDetailsResponse](#executionskilldetailsresponse)。下例为无执行数据的结构示例，编号、名称为占位值；明细元素见后续字段表。

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

## 5. 数据模型与字段说明

请求模型的“必填”包括DTO注解及应用层业务校验；响应模型的“可空”不表示字段可以省略。对象或数组元素的完整字段以链接模型为准。

### ReportFilterRequest

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `bbkId` | string | 否 | 分行编号；请求未传或null表示全部 |
| `orgId` | string | 否 | 支行编号；请求未传或null表示全部 |
| `skillIds` | array&lt;string&gt; | 否 | 技能编号数组，多选取或；未传、null或空数组表示全部 |

### ReportFiltersResponse

| 字段 | 类型 | 可空 | 说明 |
| --- | --- | --- | --- |
| `branches` | array&lt;[ReportOption](#reportoption)&gt; | 否 | 分行数组 |
| `subBranches` | array&lt;[ReportOption](#reportoption)&gt; | 否 | 支行候选项数组 |
| `skills` | array&lt;[ReportOption](#reportoption)&gt; | 否 | 技能数组；具体元素字段见对应类型 |
| `levels` | array&lt;string&gt; | 否 | 客户经理等级字符串数组 |

### ReportOption

| 字段 | 类型 | 可空 | 说明 |
| --- | --- | --- | --- |
| `id` | string | 是 | 编号，按编号识别对象 |
| `name` | string | 是 | 展示名称 |

### ReportSummaryRequest

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `bbkId` | string | 否 | 分行编号；请求未传或null表示全部 |
| `orgId` | string | 否 | 支行编号；请求未传或null表示全部 |
| `level` | string | 否 | 客户经理等级，使用数据库实际值 |
| `skillIds` | array&lt;string&gt; | 否 | 技能编号数组，多选取或；未传、null或空数组表示全部 |
| `startDate` | string(date) | 是 | 排程开始日期，yyyy-MM-dd，含当日 |
| `endDate` | string(date) | 是 | 排程结束日期，yyyy-MM-dd，含当日，不得早于开始日期 |

### ReportSummaryResponse

| 字段 | 类型 | 可空 | 说明 |
| --- | --- | --- | --- |
| `planCount` | integer(int64) | 否 | 规划数，按规划编号去重 |
| `skillCount` | integer(int64) | 否 | 技能数，按技能编号去重 |
| `taskCount` | integer(int64) | 否 | 任务数；名单任务按trace_id、方案任务按(trace_id,custuid)去重后相加 |
| `recommendedCustomerCount` | integer(int64) | 否 | 推荐客户数，非空custuid在当前范围整体去重 |
| `coveredSubBranchCount` | integer(int64) | 否 | 覆盖支行数，按(分行编号,支行编号)去重 |
| `mostUsedSkill` | [MostUsedSkillResponse](#mostusedskillresponse) | 是 | 使用人数最多的技能；并列按技能编号升序取第一项，无数据为null |
| `averageTaskCount` | integer(int64) | 是 | 人均任务数，四舍五入为整数，无人员为null |
| `averageRecommendedCustomerCount` | integer(int64) | 是 | 人均推荐客户数，四舍五入为整数，无人员为null |
| `viewRate` | number | 是 | 方案被查看率，百分比数值 |
| `contactRate` | number | 是 | 接触率，百分比数值，可能超过100 |
| `benchmarkViewRate` | number | 是 | 机构方案查看基准率 |
| `benchmarkContactRate` | number | 是 | 机构接触基准率 |
| `viewRateDifference` | number | 是 | 查看率减机构基准率，单位百分点 |
| `contactRateDifference` | number | 是 | 接触率减机构基准率，单位百分点 |

### MostUsedSkillResponse

| 字段 | 类型 | 可空 | 说明 |
| --- | --- | --- | --- |
| `id` | string | 是 | 编号，按编号识别对象 |
| `name` | string | 是 | 展示名称 |
| `userCount` | integer(int64) | 否 | 该技能使用人数，按工号去重 |

### PlanCoverageResponse

| 字段 | 类型 | 可空 | 说明 |
| --- | --- | --- | --- |
| `branches` | array&lt;[PlanCoverageRow](#plancoveragerow)&gt; | 否 | 分行数组 |

### PlanCoverageRow

| 字段 | 类型 | 可空 | 说明 |
| --- | --- | --- | --- |
| `id` | string | 是 | 编号，按编号识别对象 |
| `name` | string | 是 | 展示名称 |
| `myCount` | integer(int64) | 否 | 我的关注来源规划数 |
| `branchCount` | integer(int64) | 否 | 分行关注来源规划数，数据库来源为分行中台 |
| `presidentCount` | integer(int64) | 否 | 行长关注来源规划数 |
| `planCount` | integer(int64) | 否 | 规划数，按规划编号去重 |
| `subBranchCount` | integer(int64) | 否 | 覆盖支行数量，按机构组合去重 |
| `children` | array&lt;[PlanCoverageRow](#plancoveragerow)&gt; | 否 | 下属支行数组，支行节点为空数组 |

### PlanDetailsRequest

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `filters` | [ReportSummaryRequest](#reportsummaryrequest) | 是 | 顶部检索条件，嵌套日期必填 |
| `scopeBbkId` | string | 是 | 点击机构的分行编号，须在顶部机构范围内 |
| `scopeOrgId` | string | 否 | 点击机构的支行编号，须与分行匹配且在顶部范围内；省略为分行下钻 |
| `source` | string | 否 | 规划来源；全部、我的关注、分行关注、行长关注，未传或null表示全部 |

### PlanDetailsResponse

| 字段 | 类型 | 可空 | 说明 |
| --- | --- | --- | --- |
| `bbkId` | string | 是 | 分行编号 |
| `orgId` | string | 是 | 支行编号；规划分行下钻可为null |
| `name` | string | 是 | 展示名称 |
| `planCount` | integer(int64) | 否 | 规划数，按规划编号去重 |
| `recommendedCustomerCount` | integer(int64) | 否 | 推荐客户数，非空custuid在当前范围整体去重 |
| `taskCount` | integer(int64) | 否 | 任务数；名单任务按trace_id、方案任务按(trace_id,custuid)去重后相加 |
| `skillCount` | integer(int64) | 否 | 技能数，按技能编号去重 |
| `myCount` | integer(int64) | 否 | 我的关注来源规划数 |
| `branchCount` | integer(int64) | 否 | 分行关注来源规划数，数据库来源为分行中台 |
| `presidentCount` | integer(int64) | 否 | 行长关注来源规划数 |
| `plans` | array&lt;[PlanDetailRow](#plandetailrow)&gt; | 否 | 规划明细数组 |

### PlanDetailRow

| 字段 | 类型 | 可空 | 说明 |
| --- | --- | --- | --- |
| `id` | string | 是 | 编号，按编号识别对象 |
| `name` | string | 是 | 展示名称 |
| `source` | string | 是 | 规划来源名称：我的关注、分行关注、行长关注 |
| `ownerId` | string | 是 | 规划创建人工号（负责人） |
| `ownerName` | string | 是 | 规划创建人姓名 |
| `ownerLevel` | string | 是 | 规划创建人等级 |
| `periodStart` | string | 是 | 规划有效期开始值，仅展示，不用作统计日期 |
| `periodEnd` | string | 是 | 规划有效期结束值，仅展示，不用作统计日期 |
| `skills` | array&lt;[ReportOption](#reportoption)&gt; | 否 | 技能数组；具体元素字段见对应类型 |
| `recommendedCustomerCount` | integer(int64) | 否 | 推荐客户数，非空custuid在当前范围整体去重 |
| `taskCount` | integer(int64) | 否 | 任务数；名单任务按trace_id、方案任务按(trace_id,custuid)去重后相加 |

### ExecutionOverviewResponse

| 字段 | 类型 | 可空 | 说明 |
| --- | --- | --- | --- |
| `taskCount` | integer(int64) | 否 | 任务数；名单任务按trace_id、方案任务按(trace_id,custuid)去重后相加 |
| `recommendedCustomerCount` | integer(int64) | 否 | 推荐客户数，非空custuid在当前范围整体去重 |
| `viewCustomerCount` | integer(int64) | 否 | 方案被查看客户数 |
| `insightCustomerCount` | integer(int64) | 否 | 点击洞察按钮的推荐客户数，button_type=insight |
| `callCustomerCount` | integer(int64) | 否 | 点击电访按钮的推荐客户数，button_type=phone |
| `contactCustomerCount` | integer(int64) | 否 | 接触客户数，沿用顶部接触口径 |
| `viewRate` | number | 是 | 方案被查看率，百分比数值 |
| `insightRate` | number | 是 | 洞察率，百分比数值 |
| `callRate` | number | 是 | 电访率，百分比数值 |
| `contactRate` | number | 是 | 接触率，百分比数值，可能超过100 |
| `managerCount` | integer(int64) | 否 | 执行客户经理人数，按工号去重 |
| `customersPerTask` | number | 是 | 推荐客户数÷任务数，保留1位小数；任务数为0时null |
| `averageTaskCount` | integer(int64) | 是 | 人均任务数，四舍五入为整数，无人员为null |
| `averageRecommendedCustomerCount` | integer(int64) | 是 | 人均推荐客户数，四舍五入为整数，无人员为null |
| `averageContactCustomerCount` | integer(int64) | 是 | 人均接触客户数，四舍五入为整数，无人员为null |
| `largestLoss` | [ExecutionLossResponse](#executionlossresponse) | 是 | 最小相邻流失率对应环节；全部分母为0时null |

### ExecutionLossResponse

| 字段 | 类型 | 可空 | 说明 |
| --- | --- | --- | --- |
| `fromStage` | string | 是 | 上一阶段，view/insight/call |
| `toStage` | string | 是 | 当前阶段，insight/call/contact |
| `customerDifference` | integer(int64) | 否 | 上一阶段客户数－当前阶段客户数，允许负值 |
| `lossRate` | number | 是 | 相邻流失率，百分比数值，允许负值 |

### ExecutionBranchesRequest

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `filters` | [ReportSummaryRequest](#reportsummaryrequest) | 是 | 顶部检索条件，嵌套日期必填 |
| `sortKey` | string | 否 | 排序字段，默认taskCount；支持skillCount、taskCount、recommendedCustomerCount、viewRate、insightRate、callRate、contactRate |
| `sortDir` | string | 否 | 排序方向asc/desc，默认desc |

### ExecutionBranchesResponse

| 字段 | 类型 | 可空 | 说明 |
| --- | --- | --- | --- |
| `benchmark` | [ExecutionBenchmarkResponse](#executionbenchmarkresponse) | 否 | 保留日期和分行条件重算的四项机构基准率 |
| `rows` | array&lt;[ExecutionBranchRow](#executionbranchrow)&gt; | 否 | 明细数组；无数据为空数组 |

### ExecutionBenchmarkResponse

| 字段 | 类型 | 可空 | 说明 |
| --- | --- | --- | --- |
| `viewRate` | number | 是 | 方案被查看率，百分比数值 |
| `insightRate` | number | 是 | 洞察率，百分比数值 |
| `callRate` | number | 是 | 电访率，百分比数值 |
| `contactRate` | number | 是 | 接触率，百分比数值，可能超过100 |

### ExecutionBranchRow

| 字段 | 类型 | 可空 | 说明 |
| --- | --- | --- | --- |
| `bbkId` | string | 是 | 分行编号 |
| `bbkName` | string | 是 | 分行名称 |
| `orgId` | string | 是 | 支行编号；规划分行下钻可为null |
| `orgName` | string | 是 | 支行名称 |
| `skillCount` | integer(int64) | 否 | 技能数，按技能编号去重 |
| `taskCount` | integer(int64) | 否 | 任务数；名单任务按trace_id、方案任务按(trace_id,custuid)去重后相加 |
| `recommendedCustomerCount` | integer(int64) | 否 | 推荐客户数，非空custuid在当前范围整体去重 |
| `viewCustomerCount` | integer(int64) | 否 | 方案被查看客户数 |
| `insightCustomerCount` | integer(int64) | 否 | 点击洞察按钮的推荐客户数，button_type=insight |
| `callCustomerCount` | integer(int64) | 否 | 点击电访按钮的推荐客户数，button_type=phone |
| `contactCustomerCount` | integer(int64) | 否 | 接触客户数，沿用顶部接触口径 |
| `viewRate` | number | 是 | 方案被查看率，百分比数值 |
| `insightRate` | number | 是 | 洞察率，百分比数值 |
| `callRate` | number | 是 | 电访率，百分比数值 |
| `contactRate` | number | 是 | 接触率，百分比数值，可能超过100 |
| `viewRateDifference` | number | 是 | 查看率减机构基准率，单位百分点 |
| `insightRateDifference` | number | 是 | 洞察率减机构基准率，单位百分点 |
| `callRateDifference` | number | 是 | 电访率减机构基准率，单位百分点 |
| `contactRateDifference` | number | 是 | 接触率减机构基准率，单位百分点 |
| `viewRateStatus` | string | 是 | 查看率评价high/low/normal；不可比较为null |
| `insightRateStatus` | string | 是 | 洞察率评价high/low/normal；不可比较为null |
| `callRateStatus` | string | 是 | 电访率评价high/low/normal；不可比较为null |
| `contactRateStatus` | string | 是 | 接触率评价high/low/normal；不可比较为null |

### ExecutionDetailsRequest

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `filters` | [ReportSummaryRequest](#reportsummaryrequest) | 是 | 顶部检索条件，嵌套日期必填 |
| `scopeBbkId` | string | 是 | 点击机构的分行编号，须在顶部机构范围内 |
| `scopeOrgId` | string | 是 | 点击机构的支行编号，须与分行匹配且在顶部范围内 |

### ExecutionManagerDetailsResponse

| 字段 | 类型 | 可空 | 说明 |
| --- | --- | --- | --- |
| `bbkId` | string | 是 | 分行编号 |
| `bbkName` | string | 是 | 分行名称 |
| `orgId` | string | 是 | 支行编号；规划分行下钻可为null |
| `orgName` | string | 是 | 支行名称 |
| `summary` | [ExecutionManagerSummary](#executionmanagersummary) | 否 | 当前点击支行整体重新去重的汇总 |
| `rows` | array&lt;[ExecutionManagerRow](#executionmanagerrow)&gt; | 否 | 明细数组；无数据为空数组 |

### ExecutionManagerSummary

| 字段 | 类型 | 可空 | 说明 |
| --- | --- | --- | --- |
| `managerCount` | integer(int64) | 否 | 执行客户经理人数，按工号去重 |
| `taskCount` | integer(int64) | 否 | 任务数；名单任务按trace_id、方案任务按(trace_id,custuid)去重后相加 |
| `recommendedCustomerCount` | integer(int64) | 否 | 推荐客户数，非空custuid在当前范围整体去重 |
| `contactRate` | number | 是 | 接触率，百分比数值，可能超过100 |

### ExecutionManagerRow

| 字段 | 类型 | 可空 | 说明 |
| --- | --- | --- | --- |
| `sapId` | string | 是 | 客户经理工号 |
| `userName` | string | 是 | 客户经理姓名 |
| `level` | string | 是 | 客户经理等级，使用数据库实际值 |
| `skills` | array&lt;[ExecutionSkillOption](#executionskilloption)&gt; | 否 | 技能数组；具体元素字段见对应类型 |
| `taskCount` | integer(int64) | 否 | 任务数；名单任务按trace_id、方案任务按(trace_id,custuid)去重后相加 |
| `recommendedCustomerCount` | integer(int64) | 否 | 推荐客户数，非空custuid在当前范围整体去重 |
| `viewRate` | number | 是 | 方案被查看率，百分比数值 |
| `insightRate` | number | 是 | 洞察率，百分比数值 |
| `callRate` | number | 是 | 电访率，百分比数值 |
| `contactRate` | number | 是 | 接触率，百分比数值，可能超过100 |

### ExecutionSkillOption

| 字段 | 类型 | 可空 | 说明 |
| --- | --- | --- | --- |
| `skillId` | string | 是 | 技能编号 |
| `skillName` | string | 是 | 技能名称 |

### ExecutionSkillDetailsResponse

| 字段 | 类型 | 可空 | 说明 |
| --- | --- | --- | --- |
| `bbkId` | string | 是 | 分行编号 |
| `bbkName` | string | 是 | 分行名称 |
| `orgId` | string | 是 | 支行编号；规划分行下钻可为null |
| `orgName` | string | 是 | 支行名称 |
| `summary` | [ExecutionSkillSummary](#executionskillsummary) | 否 | 当前点击支行整体重新去重的汇总 |
| `rows` | array&lt;[ExecutionSkillRow](#executionskillrow)&gt; | 否 | 明细数组；无数据为空数组 |

### ExecutionSkillSummary

| 字段 | 类型 | 可空 | 说明 |
| --- | --- | --- | --- |
| `skillCount` | integer(int64) | 否 | 技能数，按技能编号去重 |
| `managerCount` | integer(int64) | 否 | 执行客户经理人数，按工号去重 |
| `taskCount` | integer(int64) | 否 | 任务数；名单任务按trace_id、方案任务按(trace_id,custuid)去重后相加 |
| `planCount` | integer(int64) | 否 | 规划数，按规划编号去重 |

### ExecutionSkillRow

| 字段 | 类型 | 可空 | 说明 |
| --- | --- | --- | --- |
| `skillId` | string | 是 | 技能编号 |
| `skillName` | string | 是 | 技能名称 |
| `categoryName` | string | 是 | 技能分类名称；按category_id及相同source_id关联分类表name，未配置、记录不存在或入口不一致时为null |
| `managerCount` | integer(int64) | 否 | 执行客户经理人数，按工号去重 |
| `taskCount` | integer(int64) | 否 | 任务数；名单任务按trace_id、方案任务按(trace_id,custuid)去重后相加 |
| `recommendedCustomerCount` | integer(int64) | 否 | 推荐客户数，非空custuid在当前范围整体去重 |
| `viewRate` | number | 是 | 方案被查看率，百分比数值 |
| `contactRate` | number | 是 | 接触率，百分比数值，可能超过100 |
| `planCount` | integer(int64) | 否 | 规划数，按规划编号去重 |

## 6. 调用示例

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

## 7. 数据口径与验证记录

- [顶部检索与统计口径](report-api-requirements.md)
- [规划覆盖口径](report-plan-coverage-requirements.md)
- [执行视角口径](report-execution-requirements.md)
- [执行视角及技能分类验证记录](report-execution-validation.md)

分类表结构见[技能分类DDL](../database/skill-categories.sql)，须按目标环境变更流程部署；接口不会自动建表。接口文档不代替需求口径和环境部署审批。
