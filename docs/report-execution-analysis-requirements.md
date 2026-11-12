# 执行走势、技能参与度与需关注支行需求

## 范围和依据

依据已批准的 [设计](superpowers/specs/2026-11-12-execution-analysis-design.md) 和 [接口实施计划](superpowers/plans/2026-11-12-execution-analysis-api.md)。公共关联、去重、事件定义沿用 [执行模块需求](report-execution-requirements.md) 和 [基础需求](report-api-requirements.md)。后续用户已授权接口开发，具体接口契约见 [接口说明](report-api.md) 的执行分析章节。

三个模块继承 startDate、endDate、bbkId、orgId、level、skillIds。日期包含首尾，使用 execute_day；技能多选取或。数量返回整数；率为数值百分比，保留两位小数，不附百分号。无分母返回 null，页面展示“—”，不冒充0%。

## 执行走势

返回所选区间每日数据，日期升序且完整覆盖，无数据日期也保留。单日显示一个点，十日区间返回十个点，不固定近七日。长区间仅减少日期标签，不丢弃数据点。

建议响应 dailyPoints 数组，每项字段如下，字段均返回、不省略：

| 字段 | 类型及空值 | 口径 |
| --- | --- | --- |
| date | string，非空 | yyyy-MM-dd，排程日期 |
| recommendedCustomerCount | integer，非空 | 当天非空推荐客户去重数 |
| viewCustomerCount | integer，非空 | is_read=1执行对应推荐客户去重数 |
| insightCustomerCount | integer，非空 | 匹配推荐客户与执行当日，button_click + insight的客户去重数 |
| callCustomerCount | integer，非空 | 同上，按钮为phone |
| contactCustomerCount | integer，非空 | 沿用关联接触明细cust_uid去重，NULL不计，空字符串计一个 |
| viewRate / insightRate / callRate / contactRate | number，可空 | 对应客户数÷当日推荐客户数×100% |

同一客户关联多个执行，一次符合条件查看即计为当天已查看；区间累计在全区间重新去重，不能累加每日客户数或平均每日率。洞察、电访表示点击，不要求先查看，不表示接通；接触不额外要求与推荐客户标识相等，率可超过100%，不得截断。

四条线必须包含方案被查看率、洞察率、电访率、接触率。悬停显示日期、对应客户数及率，保留原型客户量切换；零分母断点，不连接成虚假的零值。缺失数据能力应说明未支持，不显示为零。

核对示例：某日推荐10户，查看8户、洞察6户、电访4户、接触2户，四率为80、60、40、20。推荐0户时四率均为null。推荐10、接触12时接触率120。一个客户连续两日被推荐，每日各计1，区间累计仅计1。

## 技能参与度 TOPN

参与人数：区间内收到该技能任务的人群中，至少一次对应技能执行is_read=1的去重客户经理数。应执行人数：区间内收到该技能任务的去重客户经理数，包含未执行者。按sap_id去重，同一人可分别参与多个技能，不能将人员全部任务归入每个关联技能。

### 排名入参建议

| 字段 | 类型 | 必填/默认 | 校验 |
| --- | --- | --- | --- |
| topN | integer | 可选，省略或null默认5，用户已确认 | 正整数，不设额外最大值，受整数类型范围限制 |
| sortKey | string | 可选，省略默认participantCount | participantCount、participationRate、assignedManagerCount之一 |

按选定字段降序；同值按技能编号升序；参与率以未舍入值排序；先排序后取前N项。N大于合格技能数返回实际项数，不补位。分母为零不入榜，有分母无人查看显示0%。标题随请求N更新。

建议响应rows，每项字段均返回：skillId（string，非空）、skillName（string，可空，缺名称保留编号）、participantCount（integer，非空）、assignedManagerCount（integer，非空）、participationRate（number，百分比；入榜记录分母大于零）。参与率＝participantCount÷assignedManagerCount×100%，同时展示两项人数。

示例请求附加参数：`{"topN": 10, "sortKey": "participantCount"}`。示例行：`{"skillId":"S01","skillName":"技能一","participantCount":1,"assignedManagerCount":3,"participationRate":33.33}`。3人收到任务，1人未执行、1人执行未查看、1人重复查看，结果仍为1/3；同一人在另一技能有查看不影响本技能。

排序核对示例：A为2/2、B为3/10、C为1/20。参与人数排序B/A/C；参与率排序A/B/C；应执行人数排序C/B/A。省略sortKey按B/A/C；topN=2仅取前两项。比例展示相同但未舍入值不同时按实际比例排序。

### 数据核实与已确认口径

已核对 ReportRelation、ReportExecution、ReportMapper.xml 及执行应用服务：关系模型保留sapId、skillId、jobId；执行模型read来自执行表is_read，可沿执行关系归属目标人员与技能，但无法单靠该标记证明具体操作者身份。

现有应用服务先取关系再取执行，统计数据集仅保留执行；Mapper的candidates依赖执行记录匹配排程，不能提供“未执行但已收到任务”的完整分母。后续实现必须从符合公共筛选条件的人员—技能任务关系及区间排程取得分母，不要求执行或成功推荐结果存在，不改变其他模块的执行范围。

用户已确认：“区间内存在该人员、技能任务且有对应排程”定义为收到任务；接受以该任务执行is_read=1归属目标人员参与。参与记录沿用每天最新执行选择规则，在所选区间内至少一次符合条件即计入。该业务口径不要求另外证明实际操作者身份；原分母及人员归属确认阻塞已解除，但不代表现有查询已支持分母统计。

## 需关注支行

关注对象以bbkId/orgId组合标识。使用同条件支行客户统计；机构基准保留日期和分行，取消支行、等级和技能筛选，以整体去重客户计算，不能平均支行率。

| 诊断 | 条件 |
| --- | --- |
| 使用不足 | 查看率低于对应基准，接触率不低于对应基准或无法比较 |
| 接触不足 | 接触率低于对应基准，查看率不低于对应基准或无法比较 |
| 使用与接触均不足 | 两项均低于对应基准 |

严格低于才命中，比较使用未舍入值，不采用现有表格±3pp颜色阈值。单项基准或支行率为null，该项不比较；两项均不可比不入榜。零推荐不排名，无数据返回空数组。

建议响应totalCount（integer，非空，全部命中支行数）、rows（数组，非空，最多4条）。每行包含bbkId/orgId（string，非空）、bbkName/orgName（string，可空）、diagnosis（usageLow/contactLow/bothLow，非空），以及recommendedCustomerCount、viewCustomerCount、contactCustomerCount（integer，非空），viewRate、contactRate、benchmarkViewRate、benchmarkContactRate、viewRateDifference、contactRateDifference（number，可空；率为百分比，差为百分点）。

有接触不足者优先，按接触落后百分点降序；再按推荐客户数降序、分行编号升序、支行编号升序。仅查看不足者随后，按查看落后百分点降序，其余平局规则相同。totalCount在截取前计算；5家命中时显示“共5家，展示4家”，不能写成仅4家低于基准。

响应差值统一为支行率减机构基准率，负值表示落后；排序中的“落后百分点”为基准率减支行率，不将响应差值方向反转。任一率为空时差值为null。

点击支行复用现有客户经理明细，传filters及scopeBbkId/scopeOrgId；继承原筛选，不新增页面或管理动作。不同分行相同orgId不得合并。小样本仍按规则参与，显示推荐客户数供判断，不新增未经确认的样本阈值。

诊断不声称查看发生在接触之前，不称“查看后接触转化率”，不证明方案导致接触。若推荐1户且0接触，可进入关注列表；与基准完全相同不进入对应低率判断。

## 校验、错误与交付边界

日期缺失、格式错误、倒置，topN非整数/零/负数/超出整数类型范围、非法sortKey均沿用现有参数错误10000；不新增错误码。topN省略或null默认5。机构组合无匹配数据时沿用既有查询行为返回空数据，不另增归属校验。统一响应沿用code/message/success/data，HTTP规则沿用现有接口说明；未预期异常沿用99999。实际错误消息不回显原始敏感值。

新增路由为POST /api/v1/reports/execution-trend、/skill-participation、/attention-branches；未改动既有接口契约。不新增数据库、权限、日志、导出或自动测试。本文示例为口径核对，不代表真实数据验证，原HTML未接入接口。
