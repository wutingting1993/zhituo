# 组件上下文：原始知识固定模板展示

数据来源：用户提供的接口响应附件。本文只按字段和固定章节展示附件原值，不含模型归纳、推断或本轮需求决定；不是本轮直接查询数据库的结果。字段为 null、空集合或缺省时，按原始数据保留，不赋予额外业务含义。


## 响应状态

```json
{
  "code": 200,
  "message": "成功",
  "success": true
}
```


## 页面

```json
{
  "id": "328937da8dfced87",
  "name": "智拓报表",
  "path": "/Users/natasha/Downloads/智拓报表.html",
  "description": "提供规划与执行报表的检索和统计分析，可按日期、分行、支行、客户经理等级及技能查看相关数据。"
}
```


## 组件

```json
{
  "id": "328937da8dfded87",
  "componentId": "kpiStrip",
  "name": "顶部指标统计",
  "description": "位于检索框下方，通过统计接口获取当前筛选范围的规划数、技能数、任务或客户方案数、推荐客户数、方案查看率和接触率，并提供覆盖支行、人均数量、最广使用技能及机构基准比较。"
}
```


## 业务知识


### 顶部指标业务用途


登记信息：

```json
{
  "id": "328937da8e01ed87",
  "title": "顶部指标业务用途",
  "type": "BUSINESS",
  "kind": "BUSINESS_RULE",
  "status": "CONFIRMED",
  "conditions": [
    {
      "unconditional": true,
      "condition": null,
      "exception": null,
      "replacesKnowledgeId": null
    }
  ]
}
```

正文原文：


    检索框下的指标统计组件ID为kpiStrip，数据通过统计接口获取。统计内容包括生效中规划、涉及技能、当日或周期任务数、推荐客户数、方案被查看率和接触率，以及覆盖支行、使用人数最多技能、人均数量和机构基准差值。


### 顶部指标检索行为


登记信息：

```json
{
  "id": "328937da8e02ed87",
  "title": "顶部指标检索行为",
  "type": "BUSINESS",
  "kind": "BUSINESS_RULE",
  "status": "CONFIRMED",
  "conditions": [
    {
      "unconditional": true,
      "condition": null,
      "exception": null,
      "replacesKnowledgeId": null
    }
  ]
}
```

正文原文：


    指标统计按当前日期区间、分行、支行、客户经理等级和技能条件调用接口获取结果。单日与多日分别对应当日和周期统计；机构基准用于比较当前范围与所选分行整体水平。


### 页面业务说明与接口口径差异


登记信息：

```json
{
  "id": "328937da8e0fed87",
  "title": "页面业务说明与接口口径差异",
  "type": "BUSINESS",
  "kind": "CONFLICT",
  "status": "PENDING",
  "conditions": [
    {
      "unconditional": true,
      "condition": null,
      "exception": "页面业务说明与当前后端契约并存；不得按来源日期、层级或代码事实自动裁决业务意图。",
      "replacesKnowledgeId": null
    }
  ]
}
```

正文原文：


    页面业务说明与当前后端契约存在待确认的口径差异：页面生效中规划说明按当前状态、不受日期影响且按规划负责人机构归集；后端按排程及执行范围、目标客户经理机构，不额外限制生效状态。页面任务数解释为下发任务，后端taskCount表示成功推荐客户方案数，另有scheduledTaskCount等任务指标。页面接触解释为有效接通、到店或线上互动，后端按接触明细关联去重，不附加有效接通条件。页面数据按接口获取，聚合与去重以接口业务契约为依据。后续需求澄清应明确这些业务口径的取舍，不自动修改现有实现。


### 统计日期与执行范围


登记信息：

```json
{
  "id": "328940e00584ed87",
  "title": "统计日期与执行范围",
  "type": "BUSINESS",
  "kind": "BUSINESS_RULE",
  "status": "CONFIRMED",
  "conditions": [
    {
      "unconditional": false,
      "condition": "应用于当前summary顶部统计；不自动推广到未声明遵循该规则的其他模块。",
      "exception": null,
      "replacesKnowledgeId": null
    }
  ]
}
```

正文原文：


    所选排程日期含首尾。排程表示计划安排，不等于已经执行；常规指标只统计已执行范围，任务执行补充指标包含未执行任务。


### 顶部指标筛选与去重


登记信息：

```json
{
  "id": "328940e0058aed87",
  "title": "顶部指标筛选与去重",
  "type": "BUSINESS",
  "kind": "BUSINESS_RULE",
  "status": "CONFIRMED",
  "conditions": [
    {
      "unconditional": false,
      "condition": "仅适用于本页面顶部指标统计和summary接口，其他模块以各自口径为准。",
      "exception": null,
      "replacesKnowledgeId": null
    }
  ]
}
```

正文原文：


    机构按目标客户经理归属；分行、支行、等级单选，技能多选取或。各范围独立去重，不能累加每日或子维度数值替代整体。


### 规划和覆盖支行业务口径


登记信息：

```json
{
  "id": "328940e00590ed87",
  "title": "规划和覆盖支行业务口径",
  "type": "BUSINESS",
  "kind": "METRIC_RULE",
  "status": "CONFIRMED",
  "conditions": [
    {
      "unconditional": true,
      "condition": null,
      "exception": null,
      "replacesKnowledgeId": null
    }
  ]
}
```

正文原文：


    规划数为当前执行范围的规划去重数，覆盖支行按分行和支行组合去重。当前接口不额外按生效状态或有效期筛选，不套用规划覆盖模块的三类来源限制。


### 技能数量与最广使用技能


登记信息：

```json
{
  "id": "328940e00596ed87",
  "title": "技能数量与最广使用技能",
  "type": "BUSINESS",
  "kind": "METRIC_RULE",
  "status": "CONFIRMED",
  "conditions": [
    {
      "unconditional": true,
      "condition": null,
      "exception": null,
      "replacesKnowledgeId": null
    }
  ]
}
```

正文原文：


    技能数按技能编号去重；使用人数最多技能按客户经理去重人数排名，人数并列按技能编号升序，无数据为空。


### 任务与客户方案计量口径


登记信息：

```json
{
  "id": "328940e0059ced87",
  "title": "任务与客户方案计量口径",
  "type": "BUSINESS",
  "kind": "METRIC_RULE",
  "status": "CONFIRMED",
  "conditions": [
    {
      "unconditional": true,
      "condition": null,
      "exception": null,
      "replacesKnowledgeId": null
    }
  ]
}
```

正文原文：


    成功推荐客户方案按客户方案计数，与任务数量单位不同。应执行=已执行+待执行；失败也属于已执行。待执行不区分未来未到期与到期未执行。客户方案数可以大于任务数。


### 推荐客户与人均口径


登记信息：

```json
{
  "id": "328940e005a2ed87",
  "title": "推荐客户与人均口径",
  "type": "BUSINESS",
  "kind": "METRIC_RULE",
  "status": "CONFIRMED",
  "conditions": [
    {
      "unconditional": true,
      "condition": null,
      "exception": null,
      "replacesKnowledgeId": null
    }
  ]
}
```

正文原文：


    推荐客户按当前范围整体去重，跨技能同一客户只计一次；人均客户方案数和人均推荐客户数分别除以执行客户经理人数，无人员时为空。


### 方案查看率业务口径


登记信息：

```json
{
  "id": "328940e005a8ed87",
  "title": "方案查看率业务口径",
  "type": "BUSINESS",
  "kind": "METRIC_RULE",
  "status": "CONFIRMED",
  "conditions": [
    {
      "unconditional": true,
      "condition": null,
      "exception": null,
      "replacesKnowledgeId": null
    }
  ]
}
```

正文原文：


    查看率为已查看方案客户数除以推荐客户数；客户在当前范围去重，推荐客户为0时比率为空。


### 接触率业务口径


登记信息：

```json
{
  "id": "328940e005aeed87",
  "title": "接触率业务口径",
  "type": "BUSINESS",
  "kind": "METRIC_RULE",
  "status": "CONFIRMED",
  "conditions": [
    {
      "unconditional": true,
      "condition": null,
      "exception": null,
      "replacesKnowledgeId": null
    }
  ]
}
```

正文原文：


    接触率以推荐客户数为分母，当前接口按关联接触明细中的客户去重，未附加接通等有效性条件，允许超过100%。


### 机构基准与差值业务口径


登记信息：

```json
{
  "id": "328940e005b4ed87",
  "title": "机构基准与差值业务口径",
  "type": "BUSINESS",
  "kind": "BUSINESS_RULE",
  "status": "CONFIRMED",
  "conditions": [
    {
      "unconditional": true,
      "condition": null,
      "exception": null,
      "replacesKnowledgeId": null
    }
  ]
}
```

正文原文：


    基准保留日期及分行，移除支行、等级、技能；按整体去重数据计算，不平均支行率。差值为当前率减机构基准率，单位百分点。


### 数值单位与空值约定


登记信息：

```json
{
  "id": "328940e005baed87",
  "title": "数值单位与空值约定",
  "type": "BUSINESS",
  "kind": "TECHNICAL_RULE",
  "status": "CONFIRMED",
  "conditions": [
    {
      "unconditional": true,
      "condition": null,
      "exception": null,
      "replacesKnowledgeId": null
    }
  ]
}
```

正文原文：


    数量无数据为0，分母为0时比率为空。比率返回百分比数值，百分比和百分点差值保留两位，人均值取整数。


## 技术知识


### 顶部统计SQL实现


登记信息：

```json
{
  "id": "328937da8e0eed87",
  "title": "顶部统计SQL实现",
  "type": "TECHNICAL",
  "kind": "TECHNICAL_RULE",
  "status": "CONFIRMED",
  "conditions": [
    {
      "unconditional": true,
      "condition": null,
      "exception": null,
      "replacesKnowledgeId": null
    }
  ]
}
```

正文原文：


    当前顶部统计由MySQL显式JOIN和非递归CTE完成执行范围选择、去重和聚合，仅向Java返回汇总结果。Java计算比率、精确差值并组装响应；当前范围与机构基准独立聚合。执行时间并列时按执行主键倒序取一条。已有功能对照记录不代表所有后续改动已验证，也不代表已达到性能预算。


### 执行记录选择


登记信息：

```json
{
  "id": "328940e00587ed87",
  "title": "执行记录选择",
  "type": "TECHNICAL",
  "kind": "BUSINESS_RULE",
  "status": "CONFIRMED",
  "conditions": [
    {
      "unconditional": false,
      "condition": "应用于当前summary顶部统计；不自动推广到未声明遵循该规则的其他模块。",
      "exception": null,
      "replacesKnowledgeId": null
    }
  ]
}
```

正文原文：


    实际执行日期匹配execute_day且不早于规划创建时间。每任务每天取最新执行记录；时间并列按执行主键倒序。


### 筛选参数处理


登记信息：

```json
{
  "id": "328940e0058ded87",
  "title": "筛选参数处理",
  "type": "TECHNICAL",
  "kind": "BUSINESS_RULE",
  "status": "CONFIRMED",
  "conditions": [
    {
      "unconditional": false,
      "condition": "仅适用于本页面顶部指标统计和summary接口，其他模块以各自口径为准。",
      "exception": null,
      "replacesKnowledgeId": null
    }
  ]
}
```

正文原文：


    bbkId、orgId、level省略、null、空字符串视为未筛选；skillIds省略、null、空数组表示全部。不校验分支行归属，正常执行查询。


### 规划与机构去重实现


登记信息：

```json
{
  "id": "328940e00593ed87",
  "title": "规划与机构去重实现",
  "type": "TECHNICAL",
  "kind": "METRIC_RULE",
  "status": "CONFIRMED",
  "conditions": [
    {
      "unconditional": true,
      "condition": null,
      "exception": null,
      "replacesKnowledgeId": null
    }
  ]
}
```

正文原文：


    planCount按规划编号去重；coveredSubBranchCount按(bbkId,orgId)组合去重；统计归属来自目标客户经理，不使用负责人机构。


### 技能名称冲突处理


登记信息：

```json
{
  "id": "328940e00599ed87",
  "title": "技能名称冲突处理",
  "type": "TECHNICAL",
  "kind": "METRIC_RULE",
  "status": "CONFIRMED",
  "conditions": [
    {
      "unconditional": true,
      "condition": null,
      "exception": null,
      "replacesKnowledgeId": null
    }
  ]
}
```

正文原文：


    同一技能编号出现多个名称时，当前SQL选取名称最小值，仅影响名称展示，不作为新的业务标识。


### 任务及成功方案实现


登记信息：

```json
{
  "id": "328940e0059fed87",
  "title": "任务及成功方案实现",
  "type": "TECHNICAL",
  "kind": "METRIC_RULE",
  "status": "CONFIRMED",
  "conditions": [
    {
      "unconditional": true,
      "condition": null,
      "exception": null,
      "replacesKnowledgeId": null
    }
  ]
}
```

正文原文：


    taskCount筛选result_type=plan、最新成功标记=1、结果状态SUC且custuid非空，按(trace_id,custuid)去重。scheduledTaskCount按目标工号、规划、技能、排程日、任务去重；存在执行即executedTaskCount，无执行为pendingTaskCount，最新status非success为failedTaskCount。


### 推荐客户与人均实现


登记信息：

```json
{
  "id": "328940e005a5ed87",
  "title": "推荐客户与人均实现",
  "type": "TECHNICAL",
  "kind": "METRIC_RULE",
  "status": "CONFIRMED",
  "conditions": [
    {
      "unconditional": true,
      "condition": null,
      "exception": null,
      "replacesKnowledgeId": null
    }
  ]
}
```

正文原文：


    recommendedCustomerCount按成功推荐结果中非空custuid去重；averageTaskCount=taskCount/userCount，averageRecommendedCustomerCount=recommendedCustomerCount/userCount。Java以BigDecimal及HALF_UP取整数。


### 方案查看率计算实现


登记信息：

```json
{
  "id": "328940e005abed87",
  "title": "方案查看率计算实现",
  "type": "TECHNICAL",
  "kind": "METRIC_RULE",
  "status": "CONFIRMED",
  "conditions": [
    {
      "unconditional": true,
      "condition": null,
      "exception": null,
      "replacesKnowledgeId": null
    }
  ]
}
```

正文原文：


    viewRate将is_read=1执行对应的成功推荐客户去重数除以recommendedCustomerCount并乘100。


### 接触明细关联实现


登记信息：

```json
{
  "id": "328940e005b1ed87",
  "title": "接触明细关联实现",
  "type": "TECHNICAL",
  "kind": "METRIC_RULE",
  "status": "CONFIRMED",
  "conditions": [
    {
      "unconditional": true,
      "condition": null,
      "exception": null,
      "replacesKnowledgeId": null
    }
  ]
}
```

正文原文：


    接触通过click_event_id关联点击；事件为button_click或preview_view且template_type=sub。trace_id匹配执行，customer_id匹配推荐客户，点击与执行同日；cust_uid忽略NULL保留空字符串，不额外等于推荐客户标识；contactRate=接触客户去重数/推荐客户数×100。


### 机构基准差值实现


登记信息：

```json
{
  "id": "328940e005b7ed87",
  "title": "机构基准差值实现",
  "type": "TECHNICAL",
  "kind": "BUSINESS_RULE",
  "status": "CONFIRMED",
  "conditions": [
    {
      "unconditional": true,
      "condition": null,
      "exception": null,
      "replacesKnowledgeId": null
    }
  ]
}
```

正文原文：


    benchmarkViewRate、benchmarkContactRate独立聚合计算；viewRateDifference及contactRateDifference使用双方未舍入值相减，任一分母为0则差值为null。


### 数值精度实现


登记信息：

```json
{
  "id": "328940e005bded87",
  "title": "数值精度实现",
  "type": "TECHNICAL",
  "kind": "TECHNICAL_RULE",
  "status": "CONFIRMED",
  "conditions": [
    {
      "unconditional": true,
      "condition": null,
      "exception": null,
      "replacesKnowledgeId": null
    }
  ]
}
```

正文原文：


    Java使用BigDecimal与HALF_UP；JSON数值不保证尾随零，差值以未舍入值计算后舍入，不使用已舍入展示值。


## 接口：POST /zhituo_stat/api/v1/reports/summary


### 接口登记信息

```json
{
  "method": "POST",
  "address": "/zhituo_stat/api/v1/reports/summary",
  "serviceCode": "zhituo_stat",
  "path": "/api/v1/reports/summary",
  "interfaceId": "328937da8e00ed87",
  "name": "顶部指标统计",
  "description": "获取当前筛选范围的顶部指标及机构基准",
  "requestPath": "/api/v1/reports/summary",
  "requestType": "application/json",
  "responseType": "application/json"
}
```


### 请求字段

```json
[
  {
    "location": "BODY",
    "fields": [
      {
        "id": "328940e00562ed87",
        "name": "bbkId",
        "path": "bbkId",
        "location": "BODY",
        "type": "string",
        "itemType": null,
        "meaning": "分行编号；请求未传、null或空字符串表示全部",
        "detail": null,
        "validation": null,
        "required": false,
        "nullable": true,
        "children": []
      },
      {
        "id": "328940e00563ed87",
        "name": "orgId",
        "path": "orgId",
        "location": "BODY",
        "type": "string",
        "itemType": null,
        "meaning": "支行编号；请求未传、null或空字符串表示全部",
        "detail": null,
        "validation": null,
        "required": false,
        "nullable": true,
        "children": []
      },
      {
        "id": "328940e00564ed87",
        "name": "level",
        "path": "level",
        "location": "BODY",
        "type": "string",
        "itemType": null,
        "meaning": "客户经理等级，使用数据库实际值",
        "detail": null,
        "validation": null,
        "required": false,
        "nullable": true,
        "children": []
      },
      {
        "id": "328940e00565ed87",
        "name": "skillIds",
        "path": "skillIds",
        "location": "BODY",
        "type": "array<string>",
        "itemType": "string",
        "meaning": "技能编号数组，多选取或；未传、null或空数组表示全部",
        "detail": "数组元素为技能编号，多个编号取或；省略、null、空数组表示全部。",
        "validation": null,
        "required": false,
        "nullable": true,
        "children": []
      },
      {
        "id": "328940e00566ed87",
        "name": "startDate",
        "path": "startDate",
        "location": "BODY",
        "type": "string",
        "itemType": null,
        "meaning": "排程开始日期，yyyy-MM-dd，含当日",
        "detail": "日期格式yyyy-MM-dd，表示排程日期。",
        "validation": "必填，yyyy-MM-dd，含当日",
        "required": true,
        "nullable": false,
        "children": []
      },
      {
        "id": "328940e00567ed87",
        "name": "endDate",
        "path": "endDate",
        "location": "BODY",
        "type": "string",
        "itemType": null,
        "meaning": "排程结束日期，yyyy-MM-dd，含当日，不得早于开始日期",
        "detail": "日期格式yyyy-MM-dd，表示排程日期。",
        "validation": "必填，yyyy-MM-dd，含当日，不得早于startDate",
        "required": true,
        "nullable": false,
        "children": []
      }
    ]
  }
]
```


### 响应字段

```json
[
  {
    "id": "328940e00568ed87",
    "name": "code",
    "path": "code",
    "location": "BODY",
    "type": "integer",
    "itemType": null,
    "meaning": "处理结果业务码",
    "detail": "业务码，成功为200。",
    "validation": null,
    "required": true,
    "nullable": false,
    "children": []
  },
  {
    "id": "328940e00569ed87",
    "name": "message",
    "path": "message",
    "location": "BODY",
    "type": "string",
    "itemType": null,
    "meaning": "处理结果说明",
    "detail": "结果说明。",
    "validation": null,
    "required": true,
    "nullable": false,
    "children": []
  },
  {
    "id": "328940e0056aed87",
    "name": "success",
    "path": "success",
    "location": "BODY",
    "type": "boolean",
    "itemType": null,
    "meaning": "是否处理成功",
    "detail": "成功为true。",
    "validation": null,
    "required": true,
    "nullable": false,
    "children": []
  },
  {
    "id": "328940e0056bed87",
    "name": "data",
    "path": "data",
    "location": "BODY",
    "type": "object",
    "itemType": null,
    "meaning": "当前筛选范围的顶部统计结果",
    "detail": "成功时为下列业务对象，失败时为null。",
    "validation": null,
    "required": true,
    "nullable": true,
    "children": [
      {
        "id": "328940e0056ced87",
        "name": "planCount",
        "path": "data.planCount",
        "location": "BODY",
        "type": "integer",
        "itemType": null,
        "meaning": "规划数量",
        "detail": "规划数，按规划编号去重。父对象非null时该字段适用。64位整数。",
        "validation": null,
        "required": true,
        "nullable": false,
        "children": []
      },
      {
        "id": "328940e0056ded87",
        "name": "skillCount",
        "path": "data.skillCount",
        "location": "BODY",
        "type": "integer",
        "itemType": null,
        "meaning": "涉及技能数量",
        "detail": "技能数，按技能编号去重。父对象非null时该字段适用。64位整数。",
        "validation": null,
        "required": true,
        "nullable": false,
        "children": []
      },
      {
        "id": "328940e0056eed87",
        "name": "taskCount",
        "path": "data.taskCount",
        "location": "BODY",
        "type": "integer",
        "itemType": null,
        "meaning": "成功推荐客户方案数量",
        "detail": "任务数；成功推荐客户方案数，仅统计plan类型成功最新结果，排除空客户标识，按(trace_id,custuid)去重。父对象非null时该字段适用。64位整数。",
        "validation": null,
        "required": true,
        "nullable": false,
        "children": []
      },
      {
        "id": "328940e0056fed87",
        "name": "scheduledTaskCount",
        "path": "data.scheduledTaskCount",
        "location": "BODY",
        "type": "integer",
        "itemType": null,
        "meaning": "应执行任务数量",
        "detail": "应执行任务数；按目标客户经理工号、规划编号、技能编号、排程日期、任务编号去重，包含未执行任务；无数据为0。父对象非null时该字段适用。64位整数。",
        "validation": null,
        "required": true,
        "nullable": false,
        "children": []
      },
      {
        "id": "328940e00570ed87",
        "name": "executedTaskCount",
        "path": "data.executedTaskCount",
        "location": "BODY",
        "type": "integer",
        "itemType": null,
        "meaning": "已执行任务数量，包含失败任务",
        "detail": "已执行任务数；应执行任务中存在执行记录，失败也算已执行；无数据为0。父对象非null时该字段适用。64位整数。",
        "validation": null,
        "required": true,
        "nullable": false,
        "children": []
      },
      {
        "id": "328940e00571ed87",
        "name": "pendingTaskCount",
        "path": "data.pendingTaskCount",
        "location": "BODY",
        "type": "integer",
        "itemType": null,
        "meaning": "待执行任务数量",
        "detail": "待执行任务数；应执行任务中无执行记录，不区分未来和已到期；无数据为0。父对象非null时该字段适用。64位整数。",
        "validation": null,
        "required": true,
        "nullable": false,
        "children": []
      },
      {
        "id": "328940e00572ed87",
        "name": "failedTaskCount",
        "path": "data.failedTaskCount",
        "location": "BODY",
        "type": "integer",
        "itemType": null,
        "meaning": "已执行任务中的失败任务数量",
        "detail": "失败任务数；已执行任务最新记录的status不为success；无数据为0。父对象非null时该字段适用。64位整数。",
        "validation": null,
        "required": true,
        "nullable": false,
        "children": []
      },
      {
        "id": "328940e00573ed87",
        "name": "recommendedCustomerCount",
        "path": "data.recommendedCustomerCount",
        "location": "BODY",
        "type": "integer",
        "itemType": null,
        "meaning": "去重推荐客户数量",
        "detail": "推荐客户数，非空custuid在当前范围整体去重。父对象非null时该字段适用。64位整数。",
        "validation": null,
        "required": true,
        "nullable": false,
        "children": []
      },
      {
        "id": "328940e00574ed87",
        "name": "coveredSubBranchCount",
        "path": "data.coveredSubBranchCount",
        "location": "BODY",
        "type": "integer",
        "itemType": null,
        "meaning": "覆盖支行数量",
        "detail": "覆盖支行数，按(分行编号,支行编号)去重。父对象非null时该字段适用。64位整数。",
        "validation": null,
        "required": true,
        "nullable": false,
        "children": []
      },
      {
        "id": "328940e00575ed87",
        "name": "mostUsedSkill",
        "path": "data.mostUsedSkill",
        "location": "BODY",
        "type": "object",
        "itemType": null,
        "meaning": "使用人数最多的技能",
        "detail": "使用人数最多的技能；并列按技能编号升序取第一项，无数据为null。父对象非null时该字段适用。",
        "validation": null,
        "required": true,
        "nullable": true,
        "children": [
          {
            "id": "328940e00576ed87",
            "name": "id",
            "path": "data.mostUsedSkill.id",
            "location": "BODY",
            "type": "string",
            "itemType": null,
            "meaning": "技能编号",
            "detail": "编号，按编号识别对象。父对象非null时该字段适用。",
            "validation": null,
            "required": true,
            "nullable": true,
            "children": []
          },
          {
            "id": "328940e00577ed87",
            "name": "name",
            "path": "data.mostUsedSkill.name",
            "location": "BODY",
            "type": "string",
            "itemType": null,
            "meaning": "技能名称",
            "detail": "展示名称。父对象非null时该字段适用。",
            "validation": null,
            "required": true,
            "nullable": true,
            "children": []
          },
          {
            "id": "328940e00578ed87",
            "name": "userCount",
            "path": "data.mostUsedSkill.userCount",
            "location": "BODY",
            "type": "integer",
            "itemType": null,
            "meaning": "使用该技能的客户经理人数",
            "detail": "该技能使用人数，按工号去重。父对象非null时该字段适用。64位整数。",
            "validation": null,
            "required": true,
            "nullable": false,
            "children": []
          }
        ]
      },
      {
        "id": "328940e00579ed87",
        "name": "averageTaskCount",
        "path": "data.averageTaskCount",
        "location": "BODY",
        "type": "integer",
        "itemType": null,
        "meaning": "人均成功推荐客户方案数量",
        "detail": "人均任务数，四舍五入为整数，无人员为null。父对象非null时该字段适用。64位整数。",
        "validation": null,
        "required": true,
        "nullable": true,
        "children": []
      },
      {
        "id": "328940e0057aed87",
        "name": "averageRecommendedCustomerCount",
        "path": "data.averageRecommendedCustomerCount",
        "location": "BODY",
        "type": "integer",
        "itemType": null,
        "meaning": "人均推荐客户数量",
        "detail": "人均推荐客户数，四舍五入为整数，无人员为null。父对象非null时该字段适用。64位整数。",
        "validation": null,
        "required": true,
        "nullable": true,
        "children": []
      },
      {
        "id": "328940e0057bed87",
        "name": "viewRate",
        "path": "data.viewRate",
        "location": "BODY",
        "type": "number",
        "itemType": null,
        "meaning": "方案被查看客户占推荐客户的比例",
        "detail": "方案被查看率，百分比数值。父对象非null时该字段适用。",
        "validation": null,
        "required": true,
        "nullable": true,
        "children": []
      },
      {
        "id": "328940e0057ced87",
        "name": "contactRate",
        "path": "data.contactRate",
        "location": "BODY",
        "type": "number",
        "itemType": null,
        "meaning": "接触客户占推荐客户的比例",
        "detail": "接触率，百分比数值，可能超过100。父对象非null时该字段适用。",
        "validation": null,
        "required": true,
        "nullable": true,
        "children": []
      },
      {
        "id": "328940e0057ded87",
        "name": "benchmarkViewRate",
        "path": "data.benchmarkViewRate",
        "location": "BODY",
        "type": "number",
        "itemType": null,
        "meaning": "机构方案查看基准率",
        "detail": "机构方案查看基准率。父对象非null时该字段适用。",
        "validation": null,
        "required": true,
        "nullable": true,
        "children": []
      },
      {
        "id": "328940e0057eed87",
        "name": "benchmarkContactRate",
        "path": "data.benchmarkContactRate",
        "location": "BODY",
        "type": "number",
        "itemType": null,
        "meaning": "机构接触基准率",
        "detail": "机构接触基准率。父对象非null时该字段适用。",
        "validation": null,
        "required": true,
        "nullable": true,
        "children": []
      },
      {
        "id": "328940e0057fed87",
        "name": "viewRateDifference",
        "path": "data.viewRateDifference",
        "location": "BODY",
        "type": "number",
        "itemType": null,
        "meaning": "当前查看率与机构基准的差值",
        "detail": "查看率减机构基准率，单位百分点。父对象非null时该字段适用。",
        "validation": null,
        "required": true,
        "nullable": true,
        "children": []
      },
      {
        "id": "328940e00580ed87",
        "name": "contactRateDifference",
        "path": "data.contactRateDifference",
        "location": "BODY",
        "type": "number",
        "itemType": null,
        "meaning": "当前接触率与机构基准的差值",
        "detail": "接触率减机构基准率，单位百分点。父对象非null时该字段适用。",
        "validation": null,
        "required": true,
        "nullable": true,
        "children": []
      }
    ]
  }
]
```


### 接口关联规则

```json
[
  {
    "knowledgeId": "328937da8e0eed87",
    "title": "顶部统计SQL实现",
    "type": "TECHNICAL",
    "kind": "TECHNICAL_RULE",
    "content": "当前顶部统计由MySQL显式JOIN和非递归CTE完成执行范围选择、去重和聚合，仅向Java返回汇总结果。Java计算比率、精确差值并组装响应；当前范围与机构基准独立聚合。执行时间并列时按执行主键倒序取一条。已有功能对照记录不代表所有后续改动已验证，也不代表已达到性能预算。",
    "status": "CONFIRMED"
  },
  {
    "knowledgeId": "328937da8e0fed87",
    "title": "页面业务说明与接口口径差异",
    "type": "BUSINESS",
    "kind": "CONFLICT",
    "content": "页面业务说明与当前后端契约存在待确认的口径差异：页面生效中规划说明按当前状态、不受日期影响且按规划负责人机构归集；后端按排程及执行范围、目标客户经理机构，不额外限制生效状态。页面任务数解释为下发任务，后端taskCount表示成功推荐客户方案数，另有scheduledTaskCount等任务指标。页面接触解释为有效接通、到店或线上互动，后端按接触明细关联去重，不附加有效接通条件。页面数据按接口获取，聚合与去重以接口业务契约为依据。后续需求澄清应明确这些业务口径的取舍，不自动修改现有实现。",
    "status": "PENDING"
  },
  {
    "knowledgeId": "328940e00584ed87",
    "title": "统计日期与执行范围",
    "type": "BUSINESS",
    "kind": "BUSINESS_RULE",
    "content": "所选排程日期含首尾。排程表示计划安排，不等于已经执行；常规指标只统计已执行范围，任务执行补充指标包含未执行任务。",
    "status": "CONFIRMED"
  },
  {
    "knowledgeId": "328940e00587ed87",
    "title": "执行记录选择",
    "type": "TECHNICAL",
    "kind": "BUSINESS_RULE",
    "content": "实际执行日期匹配execute_day且不早于规划创建时间。每任务每天取最新执行记录；时间并列按执行主键倒序。",
    "status": "CONFIRMED"
  },
  {
    "knowledgeId": "328940e0058aed87",
    "title": "顶部指标筛选与去重",
    "type": "BUSINESS",
    "kind": "BUSINESS_RULE",
    "content": "机构按目标客户经理归属；分行、支行、等级单选，技能多选取或。各范围独立去重，不能累加每日或子维度数值替代整体。",
    "status": "CONFIRMED"
  },
  {
    "knowledgeId": "328940e0058ded87",
    "title": "筛选参数处理",
    "type": "TECHNICAL",
    "kind": "BUSINESS_RULE",
    "content": "bbkId、orgId、level省略、null、空字符串视为未筛选；skillIds省略、null、空数组表示全部。不校验分支行归属，正常执行查询。",
    "status": "CONFIRMED"
  },
  {
    "knowledgeId": "328940e00590ed87",
    "title": "规划和覆盖支行业务口径",
    "type": "BUSINESS",
    "kind": "METRIC_RULE",
    "content": "规划数为当前执行范围的规划去重数，覆盖支行按分行和支行组合去重。当前接口不额外按生效状态或有效期筛选，不套用规划覆盖模块的三类来源限制。",
    "status": "CONFIRMED"
  },
  {
    "knowledgeId": "328940e00593ed87",
    "title": "规划与机构去重实现",
    "type": "TECHNICAL",
    "kind": "METRIC_RULE",
    "content": "planCount按规划编号去重；coveredSubBranchCount按(bbkId,orgId)组合去重；统计归属来自目标客户经理，不使用负责人机构。",
    "status": "CONFIRMED"
  },
  {
    "knowledgeId": "328940e00596ed87",
    "title": "技能数量与最广使用技能",
    "type": "BUSINESS",
    "kind": "METRIC_RULE",
    "content": "技能数按技能编号去重；使用人数最多技能按客户经理去重人数排名，人数并列按技能编号升序，无数据为空。",
    "status": "CONFIRMED"
  },
  {
    "knowledgeId": "328940e00599ed87",
    "title": "技能名称冲突处理",
    "type": "TECHNICAL",
    "kind": "METRIC_RULE",
    "content": "同一技能编号出现多个名称时，当前SQL选取名称最小值，仅影响名称展示，不作为新的业务标识。",
    "status": "CONFIRMED"
  },
  {
    "knowledgeId": "328940e0059ced87",
    "title": "任务与客户方案计量口径",
    "type": "BUSINESS",
    "kind": "METRIC_RULE",
    "content": "成功推荐客户方案按客户方案计数，与任务数量单位不同。应执行=已执行+待执行；失败也属于已执行。待执行不区分未来未到期与到期未执行。客户方案数可以大于任务数。",
    "status": "CONFIRMED"
  },
  {
    "knowledgeId": "328940e0059fed87",
    "title": "任务及成功方案实现",
    "type": "TECHNICAL",
    "kind": "METRIC_RULE",
    "content": "taskCount筛选result_type=plan、最新成功标记=1、结果状态SUC且custuid非空，按(trace_id,custuid)去重。scheduledTaskCount按目标工号、规划、技能、排程日、任务去重；存在执行即executedTaskCount，无执行为pendingTaskCount，最新status非success为failedTaskCount。",
    "status": "CONFIRMED"
  },
  {
    "knowledgeId": "328940e005a2ed87",
    "title": "推荐客户与人均口径",
    "type": "BUSINESS",
    "kind": "METRIC_RULE",
    "content": "推荐客户按当前范围整体去重，跨技能同一客户只计一次；人均客户方案数和人均推荐客户数分别除以执行客户经理人数，无人员时为空。",
    "status": "CONFIRMED"
  },
  {
    "knowledgeId": "328940e005a5ed87",
    "title": "推荐客户与人均实现",
    "type": "TECHNICAL",
    "kind": "METRIC_RULE",
    "content": "recommendedCustomerCount按成功推荐结果中非空custuid去重；averageTaskCount=taskCount/userCount，averageRecommendedCustomerCount=recommendedCustomerCount/userCount。Java以BigDecimal及HALF_UP取整数。",
    "status": "CONFIRMED"
  },
  {
    "knowledgeId": "328940e005a8ed87",
    "title": "方案查看率业务口径",
    "type": "BUSINESS",
    "kind": "METRIC_RULE",
    "content": "查看率为已查看方案客户数除以推荐客户数；客户在当前范围去重，推荐客户为0时比率为空。",
    "status": "CONFIRMED"
  },
  {
    "knowledgeId": "328940e005abed87",
    "title": "方案查看率计算实现",
    "type": "TECHNICAL",
    "kind": "METRIC_RULE",
    "content": "viewRate将is_read=1执行对应的成功推荐客户去重数除以recommendedCustomerCount并乘100。",
    "status": "CONFIRMED"
  },
  {
    "knowledgeId": "328940e005aeed87",
    "title": "接触率业务口径",
    "type": "BUSINESS",
    "kind": "METRIC_RULE",
    "content": "接触率以推荐客户数为分母，当前接口按关联接触明细中的客户去重，未附加接通等有效性条件，允许超过100%。",
    "status": "CONFIRMED"
  },
  {
    "knowledgeId": "328940e005b1ed87",
    "title": "接触明细关联实现",
    "type": "TECHNICAL",
    "kind": "METRIC_RULE",
    "content": "接触通过click_event_id关联点击；事件为button_click或preview_view且template_type=sub。trace_id匹配执行，customer_id匹配推荐客户，点击与执行同日；cust_uid忽略NULL保留空字符串，不额外等于推荐客户标识；contactRate=接触客户去重数/推荐客户数×100。",
    "status": "CONFIRMED"
  },
  {
    "knowledgeId": "328940e005b4ed87",
    "title": "机构基准与差值业务口径",
    "type": "BUSINESS",
    "kind": "BUSINESS_RULE",
    "content": "基准保留日期及分行，移除支行、等级、技能；按整体去重数据计算，不平均支行率。差值为当前率减机构基准率，单位百分点。",
    "status": "CONFIRMED"
  },
  {
    "knowledgeId": "328940e005b7ed87",
    "title": "机构基准差值实现",
    "type": "TECHNICAL",
    "kind": "BUSINESS_RULE",
    "content": "benchmarkViewRate、benchmarkContactRate独立聚合计算；viewRateDifference及contactRateDifference使用双方未舍入值相减，任一分母为0则差值为null。",
    "status": "CONFIRMED"
  },
  {
    "knowledgeId": "328940e005baed87",
    "title": "数值单位与空值约定",
    "type": "BUSINESS",
    "kind": "TECHNICAL_RULE",
    "content": "数量无数据为0，分母为0时比率为空。比率返回百分比数值，百分比和百分点差值保留两位，人均值取整数。",
    "status": "CONFIRMED"
  },
  {
    "knowledgeId": "328940e005bded87",
    "title": "数值精度实现",
    "type": "TECHNICAL",
    "kind": "TECHNICAL_RULE",
    "content": "Java使用BigDecimal与HALF_UP；JSON数值不保证尾随零，差值以未舍入值计算后舍入，不使用已舍入展示值。",
    "status": "CONFIRMED"
  }
]
```


### 错误约定

```json
[
  {
    "id": "328940e00582ed87",
    "code": "10000",
    "description": "参数校验失败",
    "triggerCondition": "日期缺失、格式错误或开始日期晚于结束日期",
    "httpStatus": 200
  },
  {
    "id": "328940e00583ed87",
    "code": "99999",
    "description": "处理失败，请稍后重试",
    "triggerCondition": "非预期内部异常",
    "httpStatus": 200
  }
]
```


### 已登记示例

```json
[
  {
    "id": "328940e00581ed87",
    "name": "2026-11-11顶部统计实际调用",
    "description": "2026-11-13使用本机已有数据调用；业务日期2026-11-11，不是固定返回值。",
    "request": {
      "startDate": "2026-11-11",
      "endDate": "2026-11-11"
    },
    "response": {
      "code": 200,
      "message": "成功",
      "data": {
        "planCount": 8,
        "skillCount": 3,
        "taskCount": 15,
        "scheduledTaskCount": 13,
        "executedTaskCount": 11,
        "pendingTaskCount": 2,
        "failedTaskCount": 1,
        "recommendedCustomerCount": 8,
        "coveredSubBranchCount": 4,
        "mostUsedSkill": {
          "id": "3286c1a62df4ed87",
          "name": "同名验证技能",
          "userCount": 2
        },
        "averageTaskCount": 4,
        "averageRecommendedCustomerCount": 2,
        "viewRate": 0.0,
        "contactRate": 0.0,
        "benchmarkViewRate": 0.0,
        "benchmarkContactRate": 0.0,
        "viewRateDifference": 0.0,
        "contactRateDifference": 0.0
      },
      "success": true
    }
  }
]
```


## 关联组件

```json
[]
```


## 代码目录登记

```json
[
  {
    "id": "328937da8dfaed87",
    "systemCode": "LQ13.03",
    "projectCode": "zhituo",
    "serviceCode": null,
    "localPath": "/Users/natasha/Downloads"
  },
  {
    "id": "328937da8dfbed87",
    "systemCode": null,
    "projectCode": null,
    "serviceCode": "zhituo_stat",
    "localPath": "/Users/natasha/data/code/zhituo"
  }
]
```


## 知识缺口记录

```json
[
  {
    "type": "KNOWLEDGE_PENDING",
    "target": "328937da8e0fed87"
  },
  {
    "type": "KNOWLEDGE_PENDING",
    "target": "328937da8e0fed87"
  }
]
```
