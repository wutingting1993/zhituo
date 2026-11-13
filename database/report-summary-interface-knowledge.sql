-- 补全summary接口入参出参、字段含义、统计口径及真实调用示例，仅修改本案例接口知识。
SET NAMES utf8mb4;
START TRANSACTION;
UPDATE knowledge_entry SET content='接口：POST /api/v1/reports/summary；服务标识zhituo_stat；知识查询地址/zhituo_stat/api/v1/reports/summary。Content-Type: application/json。

请求参数字段、类型、必填及业务含义：
“必填”包括字段校验和业务校验；使用点号表示嵌套字段，`[]` 表示数组元素。

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `bbkId` | string | 否 | 分行编号；请求未传、null或空字符串表示全部 |
| `orgId` | string | 否 | 支行编号；请求未传、null或空字符串表示全部 |
| `level` | string | 否 | 客户经理等级，使用数据库实际值 |
| `skillIds` | `array<string>` | 否 | 技能编号数组，多选取或；未传、null或空数组表示全部 |
| `startDate` | string(date) | 是 | 排程开始日期，yyyy-MM-dd，含当日 |
| `endDate` | string(date) | 是 | 排程结束日期，yyyy-MM-dd，含当日，不得早于开始日期 |

请求补充约定：可选字符串省略、null或空字符串表示全部；skillIds省略、null、空数组表示全部，多个技能取或。日期是排程日期，不是请求发起日期；开始结束日期均包含当日。日期缺失、格式错误或开始日晚于结束日返回code=10000。分支行不做归属校验；不匹配时按正常查询处理。

响应字段、类型、可空及业务含义：
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

逐项统计与技术口径：
- planCount、skillCount、coveredSubBranchCount在当前执行统计范围分别按规划编号、技能编号、(分行编号,支行编号)去重。名称生效中规划不增加当前后端未实现的发布状态、有效期筛选。
- taskCount为成功推荐客户方案数：result_type=plan、最新成功结果标记=1、状态SUC、客户标识非空，按(trace_id,custuid)去重，排除客户名单；不是应执行任务数。
- scheduledTaskCount按目标客户经理工号、规划编号、技能编号、排程日期、任务编号确定应执行任务。executedTaskCount存在对应执行，失败也属于已执行；pendingTaskCount无执行，不区分未来未到期和到期未执行；failedTaskCount按最新执行status非success判定。应执行=已执行+待执行，失败为已执行子集；客户方案与任务单位不同。
- recommendedCustomerCount为当前范围成功推荐结果中非空custuid整体去重，不能累加每日或子维度去重数。
- mostUsedSkill按使用客户经理工号去重计数，使用人数并列按技能编号升序；名称冲突当前SQL取名称最小值。无技能为null。
- averageTaskCount=成功推荐客户方案数/执行客户经理人数；averageRecommendedCustomerCount=去重推荐客户数/执行客户经理人数。HALF_UP取整数，人员为0时为null。
- viewRate=查看状态is_read=1的执行对应成功推荐客户去重数/推荐客户数×100。
- contactRate=关联到合法点击事件的接触明细cust_uid去重数/推荐客户数×100。点击为button_click或preview_view且template_type=sub；点击trace_id匹配执行、customer_id匹配推荐客户且点击与执行同日；接触通过click_event_id关联，忽略NULL但保留空字符串，不额外要求接触客户标识等于推荐客户标识，不附加接通等条件，不截断到100%。
- benchmarkViewRate和benchmarkContactRate采用相同公式，但基准保留日期、分行条件，移除支行、等级、技能条件，以整体去重结果计算，不平均支行比率；未选分行为全行范围。
- viewRateDifference=viewRate未舍入值-基准查看率未舍入值；contactRateDifference同理，单位为百分点。任一侧分母为0时差值为null。
- 比率返回百分比数值，如62.5代表62.5%；百分比和百分点差值HALF_UP保留两位，JSON不保证尾随零；数量无数据为0，分母0时比率为null。
- 所选排程日期含首尾；实际执行日期匹配排程日期且执行时间不早于规划创建时间，每任务每天取最新执行，执行时间并列按执行主键倒序选一条。MySQL完成范围筛选、去重和聚合，Java计算比率、精确差值并组装响应。

响应及错误约定：应用JSON响应HTTP状态200，以code和success判断结果；成功code=200、success=true；请求校验错误10000、非预期错误99999，失败data=null。网关容器错误不受该JSON约定保证。完整错误码沿用现有ResponseEnum。

实际请求示例（2026-11-13读取本机现有排程数据后调用，业务日期2026-11-11）：
{
  "startDate": "2026-11-11",
  "endDate": "2026-11-11"
}

实际响应示例（历史真实调用结果，不是固定返回值，非生产数据声明）：
{
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
}' WHERE id='328937da8e0ded87' AND system_code='LQ13.03' AND project_code='zhituo' AND kind='API_CONTRACT';
COMMIT;
