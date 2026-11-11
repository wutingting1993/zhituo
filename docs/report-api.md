# 智拓报表接口

基础路径 `/api/v1/reports`。所有接口仅接受 POST，参数从 JSON body 读取，不读取查询参数；body 必须提供。只读接口，不建表、不写入业务数据。

## 检索选项

`POST /api/v1/reports/filters`，`Content-Type: application/json`

```json
{"bbkId":"B01","orgId":"O01","skillIds":["S01","S02"]}
```

查询全部选项时 body 传 `{}`。

所有参数均可省略。分行 `bbkId`、支行 `orgId` 为单选，`skillIds` 为多选，多个技能为或关系；省略表示全部。查询某类候选项时移除该类自身的选择条件，应用其余两类条件。日期不影响候选项。

成功时 `data` 包含 `branches`、`subBranches`、`skills`（均为 `{id,name}` 数组）及 `levels`（数据库实际等级字符串数组），无数据返回空数组。

## 顶部指标

`POST /api/v1/reports/summary`，`Content-Type: application/json`

```json
{"startDate":"2026-11-01","endDate":"2026-11-11","bbkId":"B01","orgId":"O01","level":"L1","skillIds":["S01"]}
```

`startDate`、`endDate` 必填，格式为 `yyyy-MM-dd`，包含首尾日期，开始日期不能晚于结束日期。其余参数可省略；等级按实际值筛选。

| data 字段 | 含义 |
| --- | --- |
| planCount | 规划数 |
| skillCount | 技能数 |
| taskCount | 名单任务数与方案任务数之和 |
| recommendedCustomerCount | 推荐客户数 |
| viewRate / contactRate | 方案被查看率 / 接触率，数值百分比 |
| coveredSubBranchCount | 按分行与支行组合去重的覆盖支行数 |
| mostUsedSkill | `{id,name,userCount}`，人数最多的技能，并列取编号最小项 |
| averageTaskCount / averageRecommendedCustomerCount | 人均任务数 / 人均推荐客户数，四舍五入整数 |
| benchmarkViewRate / benchmarkContactRate | 仅保留日期及分行条件的机构基准率 |
| viewRateDifference / contactRateDifference | 当前率减基准率，单位百分点 |

数量无数据为 `0`。推荐客户数为零时相应比率为 `null`；客户经理人数为零时人均值为 `null`；没有参与执行的技能时 `mostUsedSkill=null`。任一比率无分母时差值为 `null`。百分比和差值保留两位小数，差值使用未舍入值计算。接触率可能超过 100%。

数据匹配及去重口径见 [需求文档](report-api-requirements.md)。

## 分支行规划覆盖

`POST /api/v1/reports/plan-coverage`，请求体与顶部指标一致：

```json
{"startDate":"2026-11-10","endDate":"2026-11-11","bbkId":"B01","skillIds":["S01"]}
```

返回 `data.branches` 数组，每个分行包含 `children` 支行数组。行字段：`id`、`name`、`myCount`（我的关注）、`branchCount`（分行关注）、`presidentCount`（行长关注）、`planCount`、`subBranchCount`（覆盖支行数）。支行的 `children` 为空数组。分行规划数独立去重，不累加支行规划数。无数据返回 `{"branches":[]}`。

仅统计“我的关注、分行中台、行长关注”三类来源，“分行中台”展示为“分行关注”，空值和其他来源排除。数据基础范围与顶部指标一致，顶部指标不增加该来源过滤。

## 规划明细

`POST /api/v1/reports/plan-coverage/details`：

```json
{
  "filters":{"startDate":"2026-11-10","endDate":"2026-11-11","bbkId":"B01","orgId":"O01","skillIds":["S01"]},
  "scopeBbkId":"B01",
  "scopeOrgId":"O01",
  "source":"分行关注"
}
```

`filters` 必填，嵌套字段沿用顶部指标校验。`scopeBbkId` 必填；`scopeOrgId` 可省略，省略时为分行下钻，仍保留 filters 中的支行条件。点击机构必须在 filters 的机构范围内。`source` 可省略或为“全部、我的关注、分行关注、行长关注”，不接受数据库枚举“分行中台”。

返回机构 `bbkId/orgId/name`、`planCount`、`recommendedCustomerCount`、`taskCount`、`skillCount`、三类来源数量及 `plans` 数组。来源筛选仅影响 plans，不改变汇总及来源数量。空结果数量为0、plans为空数组；范围内无执行记录时机构名称为null。

每条规划包含 `id/name/source`、`ownerId/ownerName/ownerLevel`、`periodStart/periodEnd`、`skills`（`{id,name}` 数组）、`recommendedCustomerCount/taskCount`。负责人为规划创建人；技能仅返回当前筛选范围内参与执行的技能。姓名、等级、有效期字段按实际数据可为null。汇总客户跨规划去重，不累加明细客户数。

当前“今日任务”“生效中规划”统一使用顶部日期区间，后期口径可能调整；当前不增加系统当日、发布状态或有效期过滤。详细规则见 [规划覆盖需求](report-plan-coverage-requirements.md)。

## 统一响应与错误

应用可控制的正常和异常 JSON 响应 HTTP 状态均为 `200`，错误码沿用现有 `ResponseEnum`：

```json
{"code":200,"message":"成功","data":{"branches":[],"subBranches":[],"skills":[],"levels":[]},"success":true}
```

```json
{"code":10000,"message":"开始日期不能晚于结束日期","data":null,"success":false}
```

缺少日期、格式错误、日期倒置、分支行不匹配均为 `10000`；未知异常为 `99999`。其余通用错误沿用项目既有错误码，不新增报表错误码。

## 本机验证范围

使用项目 README 所列 JDK 8 启动服务，连接本机 MySQL zhituo 库。已完成空数据、参数错误验证，以及用户授权造数后的16项有数据接口验证；数据、脚本与实际结果见 [造数验证记录](report-api-validation.md)。验证数据当前保留在本机，可用记录中的精确清理脚本删除。未新增单元测试或 e2e 测试。
