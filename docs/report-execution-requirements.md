# 智拓报表接口需求：执行视角—执行链路概览与支行执行明细

## 需求范围

整理执行链路概览、支行执行明细，以及客户经理执行明细、技能明细下钻的接口需求。不开发接口、前端或数据库，不包含执行走势、技能参与度 TOP5、需关注支行及导出。

沿用 [智拓报表接口需求](report-api-requirements.md) 的基础数据范围与公共统计口径。HTML 为演示原型，不直接采用其模拟数据计算方式；不继承规划覆盖模块专有的三类规划来源限制。

## 接口与检索条件

下列路径已按本文需求实现。均使用 POST 和 JSON body。

| 接口 | 用途 | 入参 | 返回内容 |
| --- | --- | --- | --- |
| `/api/v1/reports/execution-overview` | 执行链路概览 | 顶部检索条件 | 六阶段数量、四项比率、户/任务及辅助数据 |
| `/api/v1/reports/execution-branches` | 支行执行明细 | 顶部检索条件、排序字段及方向 | 支行列表、各项指标、机构基准率及差值 |
| `/api/v1/reports/execution-branches/managers` | 客户经理执行明细 | 顶部检索条件、点击支行编号 | 支行信息、汇总及客户经理列表 |
| `/api/v1/reports/execution-branches/skills` | 技能明细 | 顶部检索条件、点击支行编号 | 支行信息、汇总及技能列表 |

- 顶部条件沿用 `startDate`、`endDate`、`bbkId`、`orgId`、`level`、`skillIds`。开始、结束日期必填，其他可选；技能多选取“或”，其他单选，未选表示全部。
- 日期使用排程 `swe_plan_sence_cron.execute_day`，包含首尾两天；开始日期不得晚于结束日期。检索选项复用现有 filters 接口。
- 人员和机构通过规划目标人员关联，分行取 `first_bbk_id`、支行取 `org_id`、等级取 `pst_lvl`；按编号筛选与去重。
- 下钻继承全部顶部条件，再限定点击支行；支行应同时携带所属分行信息。分支行不匹配、点击机构超出筛选范围时返回参数错误。
- 不新增分页、搜索或原型未要求的交互。

## 执行指标口径

各指标在整个统计范围内重新去重，不能直接累加每日、客户经理或技能数值。

| 指标 | 业务含义 | 技术口径 |
| --- | --- | --- |
| 任务下发/任务数 | 名单任务与方案任务总数 | 沿用顶部任务数：匹配成功结果后，名单任务按 `trace_id` 去重，方案任务按 `(trace_id,custuid)` 去重，两类相加；“任务下发”文案不代表统计未执行或无结果任务 |
| 名单推荐/推荐客户数 | 获得推荐的客户数 | 成功结果中的非空字符串 `custuid`，按客户去重 |
| 方案被查看客户数 | 推荐客户中方案被查看的客户数 | 执行 `is_read=1` 对应推荐客户，按 `custuid` 去重 |
| 去洞察客户数 | 点击洞察按钮的推荐客户数 | 匹配推荐客户和执行当天的点击事件，`event_type=button_click` 且 `button_type=insight`，按 `customer_id` 去重 |
| 去电访客户数 | 点击电访按钮的推荐客户数 | 与洞察相同的关联、日期及去重方式，事件要求 `event_type=button_click` 且 `button_type=phone` |
| 接触客户数 | 关联接触明细的客户数 | 沿用顶部接触口径，按接触表 `cust_uid` 去重；NULL 不计，空字符串计一个值，不额外要求与推荐客户标识相等 |
| 方案被查看率、洞察率、电访率、接触率 | 对应客户数占推荐客户数的比例 | 对应数量÷推荐客户数×100%，四项均以推荐客户数为分母，不采用上一阶段数量作为分母 |
| 涉及技能数 | 范围内参与执行的技能数 | 按 `skill_id` 去重；有执行但没有成功结果的技能仍保留，与顶部技能数一致 |

数量无数据返回0；率值返回数值百分比，四舍五入保留两位小数，推荐客户数为0时返回 null，展示“—”。接触率不强制截断到100%。洞察、电访表示点击行为，不能解释为已进入目标系统、已接通或已形成有效接触。

## 执行链路概览及辅助数据

- 顺序为任务下发→名单推荐→方案被查看→去洞察客户→去电访客户→接触客户，数量与相同条件下的顶部指标一致。
- 名单推荐与任务之间展示“户/任务”，即推荐客户数÷任务数，保留1位小数；任务数为0返回 null。
- 其余阶段展示对应客户数占推荐客户数的比率，不解释为相邻环节转化率。
- 人均任务、人均推荐、人均接触分别为对应数量÷客户经理人数，四舍五入为整数；人数按符合统计条件的目标人员 `sap_id` 去重，人数为0返回 null。
- 最大流失环节按用户确认规则：对查看→洞察、洞察→电访、电访→接触三个环节，分别计算 `(上一环节名单数－当前环节名单数)÷上一环节名单数`，取未舍入值最小的环节，展示环节名称、数量差及该比率；此处名单数指各阶段去重客户数。按用户指定取最小值，不改为最大值。上一环节为0时无法计算，该环节不参与比较；全部无法计算返回 null。并列时按链路顺序取第一项。流失率返回百分比数值并保留两位小数，选取环节使用未舍入值。数量差或比率为负时保留原值，不截断，也不额外增加逐级包含过滤。

## 支行执行明细

| 数据 | 口径及行为 |
| --- | --- |
| 支行 | 支行编号、名称、所属分行编号及名称；以分行和支行编号组合标识机构 |
| 涉及技能 | 当前支行范围内去重技能数，大于0时可点击打开技能明细 |
| 任务数 | 同执行指标口径；单日展示“当日任务数”，多日展示“周期任务数” |
| 推荐客户数 | 支行范围内整体去重 |
| 方案被查看、去洞察、去电访、接触 | 各返回客户数和比率 |
| 支行下钻 | 点击支行名称打开客户经理执行明细 |

- 支持按涉及技能数、任务数、推荐客户数、查看率、洞察率、电访率、接触率升降序排列；默认按任务数倒序，不额外引入其他排序字段。
- 机构基准保留日期及分行条件，取消支行、等级、技能条件，重新计算四项整体比率；未选分行时为全行。不能平均支行比率。
- 差值为支行率减机构基准率，单位为百分点，按未舍入值计算后保留两位小数。差值达到+3个百分点标高，达到-3个百分点标低，其余正常；高低标识使用未舍入差值判断，不使用展示值判断。任一比率为 null 时不比较。悬浮信息展示机构基准和差值。
- 无符合条件支行返回空数组，不为展示零值机构而扩大既有数据范围。

## 客户经理执行明细

标题为“支行名称·客户经理执行明细”，按任务数倒序，继承日期、等级、技能及机构条件。

| 数据 | 返回内容与口径 |
| --- | --- |
| 汇总 | 客户经理人数、任务数、推荐客户数、接触率；在整个点击支行内重新计算，与支行行数据一致 |
| 列表 | 客户经理工号及姓名、等级、涉及技能、任务数、推荐客户数、方案查看率、洞察率、电访率、接触率 |
| 人员维度 | 按目标人员 `sap_id` 分组，指标在该人员及顶部条件范围内独立去重 |
| 涉及技能 | 仅展示当前条件下实际参与执行的技能，不带出未选技能 |

同一客户可能属于多位客户经理，客户经理行的客户数不能相加作为支行汇总。无数据返回空列表，汇总数量返回0、比率分母为0返回 null。

## 技能明细

标题为“支行名称·技能明细”，按任务数倒序，统计当前条件下该支行实际执行到的技能。

| 数据 | 返回内容与口径 |
| --- | --- |
| 汇总 | 涉及技能数、执行客户经理人数、任务数、关联规划数；支行整体重新去重，不相加列表值 |
| 列表 | 技能编号及名称、技能类型、执行人数、任务数、推荐客户数、方案查看率、接触率、关联规划数 |
| 技能维度 | 按 `skill_id` 分组，指标在该技能及顶部条件内计算 |
| 执行人数 | 每个技能按目标人员工号去重，同一人可归属多个技能 |
| 关联规划 | 在相同执行统计范围内按 `plan_id` 去重，不额外限定当前发布状态或有效期 |

同一客户、人员、规划可能跨技能出现，各行不能直接相加。技能分类名称通过 `swe_marketplace_skills.category_id = swe_marketplace_categories.id` 获取 `name`，同时要求分类 `source_id` 与技能入口一致（当前统计入口为 `RMASSIST`）。未配置分类、关联分类不存在或入口不一致时返回 null，页面展示“—”，不剔除技能及统计数据。不使用场景产品大类替代，不增加 `branch_visible` 或 `sort_order` 过滤、排序条件；这两个字段不属于本次报表统计口径。无数据返回空列表，数量为0，分母为0的比率为 null。

## 数据来源与匹配规则

基础关联完整沿用 [智拓报表接口需求](report-api-requirements.md)，不另定义一套统计链路。

| 步骤 | 表与规则 |
| --- | --- |
| 人员、规划、技能 | 目标 `sap_id` 关联人员 `user_id`；规划关联目标与场景，场景 `scene_id` 关联技能 `skill_id`。规划、目标、场景要求 `deleted=0`，技能要求 `source_id=RMASSIST` |
| 任务与排程 | 任务人员、规划、技能整字段匹配；要求 `source_id=RMASSIST`、`status!=deleted`；排程日期落在所选区间内 |
| 执行 | 每任务每天按 `actual_time` 倒序取最新一次，且不早于规划创建时间；并列及状态处理沿用存量，没有执行不计入 |
| 推荐结果 | 同时匹配 `job_id`、`skill_id`、`trace_id`，要求 `result_type=plan`、`is_latest_success=1`、`status=SUC`；无结果仍保留规划、技能、执行信息，不计任务及推荐客户 |
| 洞察、电访事件 | `swe_html_preview_click_events.trace_id` 匹配选定执行，`customer_id` 匹配非空推荐 `custuid`，`clicked_at` 与执行 `actual_time` 同日；按事件及按钮类型分类，同客户多次点击只计一次，不要求必须先查看或先洞察 |
| 接触 | 点击与推荐、执行的关联沿用存量；事件为 `button_click`，或 `preview_view` 且 `template_type=sub`，点击 `id` 关联 `swe_skill_contact_detail.click_id`；不额外增加按钮类型或接触时间过滤 |

### 洞察与电访按钮取值

按用户修订后的原始《规划统计需求口径》（“八、HTML 方案点击明细表”），洞察使用 `event_type=button_click` 且 `button_type=insight`，电访使用 `event_type=button_click` 且 `button_type=phone`，与现有表结构注释一致。电访不使用按钮类型 `plan`，也不增加历史值兼容。结果表 `result_type=plan` 仍表示个人方案报告，其原有筛选规则保持不变。

## 验收要求

- 相同顶部条件下，概览与顶部任务、推荐、查看、接触统计一致；支行及下钻使用相同基础关联，不附加规划来源分类限制。
- 洞察、电访只统计匹配推荐客户和执行当日的按钮点击，跨日期、人员、技能按当前统计范围去重；重复点击不重复计数。
- 所有比率以推荐客户数为分母，机构基准整体重算，差值以百分点表达；空分母返回 null。
- 下钻保留全部顶部条件；客户经理及技能明细汇总与点击支行一致，不累加重复客户、人员、规划。
- 技能明细展示原型指定字段，不额外添加洞察、电访率等列；客户经理明细包含四项比率。
- 最大流失环节按相邻客户数差除以上一阶段客户数，取最小值；零分母不参与，全部不可计算返回 null，负值保留。并列按链路顺序取第一项。
- 数量为0、空列表、参数错误、排序、阈值边界按本文规则处理。
- 技能分类名称按分类ID及相同入口关联获取；缺失时返回 null，不影响原有技能数与执行统计。接口路径已按本文实现；本文保留需求口径，实施验证不包含未经授权的单元测试或 e2e 测试。

## 接口字段约定

沿用项目统一响应 `code`、`message`、`success`、`data`，业务数据放在 `data` 内，错误处理沿用现有接口。

### 请求

- 概览直接接收顶部条件，与现有 summary 请求一致。
- 支行列表接收 `filters`（顶部条件）、`sortKey`、`sortDir`。`sortKey` 可取 `skillCount`、`taskCount`、`recommendedCustomerCount`、`viewRate`、`insightRate`、`callRate`、`contactRate`，默认 `taskCount`；`sortDir` 为 `asc/desc`，默认 `desc`。非法枚举返回参数错误。
- 两类下钻沿用现有规划明细的命名：`filters`、`scopeBbkId`、`scopeOrgId`，两个 scope 字段均必填，独立于顶部筛选条件。
- 比率为 null 的记录在升降序中均排最后；主排序相同时，支行按分行编号、支行编号升序，客户经理按工号升序，技能按技能编号升序。

支行列表请求示例：

```json
{
  "filters": {
    "startDate": "2026-11-01",
    "endDate": "2026-11-11",
    "bbkId": "B001",
    "orgId": null,
    "level": null,
    "skillIds": []
  },
  "sortKey": "taskCount",
  "sortDir": "desc"
}
```

下钻请求使用同一 `filters` 对象，再传 `scopeBbkId: "B001"`、`scopeOrgId: "O001"`，不传排序参数。

### 响应

| 数据 | 字段约定 |
| --- | --- |
| 公共数量及比率 | `taskCount`、`recommendedCustomerCount`、`viewCustomerCount`、`insightCustomerCount`、`callCustomerCount`、`contactCustomerCount`、`viewRate`、`insightRate`、`callRate`、`contactRate` |
| 概览辅助字段 | `managerCount`、`customersPerTask`、`averageTaskCount`、`averageRecommendedCustomerCount`、`averageContactCustomerCount`、`largestLoss` |
| 最大流失环节 | `largestLoss` 包含 `fromStage`、`toStage`、`customerDifference`、`lossRate`；阶段值为 `view/insight/call/contact`；全部不可计算时整个对象为 null |
| 支行列表 | `rows` 数组；每行包含 `bbkId`、`bbkName`、`orgId`、`orgName`、`skillCount` 及公共数量、比率 |
| 机构基准 | 支行列表顶层 `benchmark`，包含 `viewRate`、`insightRate`、`callRate`、`contactRate`；每行包含 `viewRateDifference`、`insightRateDifference`、`callRateDifference`、`contactRateDifference` 及对应 `viewRateStatus/insightRateStatus/callRateStatus/contactRateStatus`（`high/low/normal`，无法比较为 null） |
| 下钻通用结构 | `bbkId`、`bbkName`、`orgId`、`orgName`、`summary`、`rows` |
| 客户经理汇总 | `summary` 包含 `managerCount`、`taskCount`、`recommendedCustomerCount`、`contactRate` |
| 客户经理行 | `sapId`、`userName`、`level`、`skills`（元素为 `skillId/skillName`），以及 `taskCount`、`recommendedCustomerCount` 和四项比率 |
| 技能汇总 | `summary` 包含 `skillCount`、`managerCount`、`taskCount`、`planCount` |
| 技能行 | `skillId`、`skillName`、`categoryName`（分类表名称，未匹配时为 null）、`managerCount`、`taskCount`、`recommendedCustomerCount`、`viewRate`、`contactRate`、`planCount` |

数量为整数，技能分类名称未匹配时明确返回 null，不省略字段；其他空值规则沿用正文。百分比以0—100为常见数值范围（如62.5表示62.5%），不返回0—1比例或带百分号字符串；接触率及流失率按正文允许超出常见范围。

概览 `data` 示例（仅演示结构及计算，不代表真实业务数据）：

```json
{
  "taskCount": 20,
  "recommendedCustomerCount": 100,
  "viewCustomerCount": 80,
  "insightCustomerCount": 60,
  "callCustomerCount": 30,
  "contactCustomerCount": 15,
  "viewRate": 80.00,
  "insightRate": 60.00,
  "callRate": 30.00,
  "contactRate": 15.00,
  "managerCount": 5,
  "customersPerTask": 5.0,
  "averageTaskCount": 4,
  "averageRecommendedCustomerCount": 20,
  "averageContactCustomerCount": 3,
  "largestLoss": {
    "fromStage": "view",
    "toStage": "insight",
    "customerDifference": 20,
    "lossRate": 25.00
  }
}
```
