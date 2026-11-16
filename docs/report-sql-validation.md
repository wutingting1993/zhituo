# 其余报表接口 SQL 聚合功能验证

2026-11-13，本机MySQL zhituo现有验证数据。仅只读查询，未造数、未插入/更新/删除数据库记录，未做性能测试。

## 改造范围

其余10个统计接口（7个执行视角、3个规划视角）改为显式JOIN和非递归CTE聚合，summary复用公共范围CTE，filters保持候选项查询。Java接收总体、日期或维度汇总和必要技能展示关系，不接收客户级推荐、点击或接触明细。

分组由ReportDimension固定枚举选择，不拼接请求提供的SQL列名。数据库先分维度去重再聚合；总体与机构基准独立计算，不累加分组客户数。Java仍负责比率、未舍入差值比较、排序、TOPN、诊断、响应树和日期补零。

特别保留：规划覆盖和来源结构只使用已执行记录；规划明细保留符合条件的待执行任务及来源数量。参与度单独聚合有区间排程的任务分母及查看客户经理分子，不读取推荐结果。规划覆盖、来源结构不读取客户行为，规划明细来源标签仍只筛选列表。

重复编号展示值使用MIN确定取值，原明细路径首条无稳定顺序。执行时间并列沿用summary已引入的主键倒序选取，仍属于允许任取一条的边界；所有接口字段、必填/可空、错误码、参数与统计公式不变。

## 功能对照

- 基线源码：/private/tmp/zhituo-report-sql-baseline，本次改造前复制的当前工作区，不是Git历史构建产物；保留原10个接口明细计算及此前summary聚合。
- 基线服务18111，改造服务18112，共用同一本机验证库；不替换已有服务。
- 实际命令：`python3 scripts/report-sql-validation/verify.py`，退出码0，**423/423通过**。
- [验证脚本](../scripts/report-sql-validation/verify.py)、[完整请求和响应](../scripts/report-sql-validation/results.json)。比较HTTP状态与完整JSON，不忽略字段、null、数组顺序或错误文案；分行summary额外断言非零规划及客户，避免两边空结果伪通过。

| 接口后缀 | 对照数量 |
| --- | ---: |
| summary | 33 |
| execution-overview | 33 |
| plan-coverage | 33 |
| plan-sources | 39 |
| execution-trend | 33 |
| attention-branches | 33 |
| execution-branches | 61 |
| skill-participation | 51 |
| execution-branches/managers | 33 |
| execution-branches/skills | 31 |
| plan-coverage/details | 41 |
| filters | 2 |

覆盖两套既有数据：全行/分行/支行、等级、单多技能、组合条件、区间首尾日期、空范围、未知技能、机构不匹配、空/null技能数组、NULL执行链路、无执行以及205名客户经理批次数据。覆盖客户经理和技能下钻、分行规划下钻、4种来源、7种支行排序及升降序、3种参与度排序与TOP1/5/100、来源TOP1/5/100，以及日期倒置、非法排序/TOPN/来源和下钻超范围。

对应计划重点：跨维度独立去重由总体/支行/客户经理/技能及规划响应对照覆盖；待执行与参与度分母由无执行及排程数据覆盖；规划来源标签由四类标签及分行下钻覆盖；精确排序、null及日期补零由排序与空日期区间覆盖。没有为极近舍入边界额外造数，不将比较相同的零值称为非零阈值专项验证。

## 静态、构建与审阅

实际使用README指定JDK8。

- `bash scripts/java-quality.sh verify`指定9个新增/修改Java文件：ReportAggregate、ReportDimension、ReportAggregatePo、ReportDataSupport、ReportMapper、ReportDataSupportImpl、ReportDataConverter、ExecutionReportApplicationServiceImpl、PlanCoverageApplicationServiceImpl，退出码0。
- `bash scripts/backend-gate.sh src/main/java/`：92个Java文件静态、格式、编译及架构检查通过，退出码0；架构检查1项，失败0、错误0。初次与格式化并行执行时发现两处长行，格式化完成后重跑全量门禁通过。
- Mapper XML解析和Python脚本ast.parse通过；未配置Python lint，不将语法检查视为完整静态检查。
- 检查两个应用服务不再调用findExecutions/findRecommendations/findActions/findContacts；新增SQL无SELECT *或客户端SQL字符串插值。
- 独立只读审阅对照基线，未发现确定的口径遗漏。

初轮服务加载了尚未替换GROUPS保留字的旧资源，execution-overview SQL报错；CTE改名report_groups并重启后完整423项通过。最终results.json仅记录成功轮次，不隐藏该修正过程。

## 限制及清理

现有数据功能一致不代表任意数据、并发稳定性或生产规模性能。未新增大小写UID、重复技能名称、同时间并列执行、非零流失并列、极近±3pp等专项数据；相关边界经过SQL审阅，但不宣称全部已运行覆盖。未增加索引、未做执行计划分析或测量提速。基线副本保留供复核；临时服务验证后停止，复验需重新启动服务。
