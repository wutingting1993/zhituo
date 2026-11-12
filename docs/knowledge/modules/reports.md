# 报表模块与实现地图

实现状态指当前工作区存在路由和实现，不等于已部署或本轮验证通过。所有路由前缀 /api/v1/reports，Controller 均声明 POST、JSON body。

| 模块 | 路由后缀 | 专有边界 |
| --- | --- | --- |
| 候选项 | filters | 当前接口文档：机构树和等级全量；技能按规划所属机构联动，不等同统计客户经理机构 |
| 顶部统计 | summary | 六指标、辅助数据和机构基准 |
| 规划覆盖及明细 | plan-coverage、plan-coverage/details | 只纳入三类来源；明细来源筛选不影响汇总 |
| 来源结构 | plan-sources | 三类来源，先筛选后按分行规划总数排名，默认Top5 |
| 执行概览及下钻 | execution-overview、execution-branches、execution-branches/managers、execution-branches/skills | 各维度独立去重，四率分母一致 |
| 执行分析 | execution-trend、skill-participation、attention-branches | 日趋势独立去重；参与度含未执行任务分母；关注列表最多4条 |

规划来源的业务名称统一为我的关注、分行关注、行长关注。其中“分行关注”当前对应数据库 source_label 历史存储值“分行中台”；其他及空来源排除。该限制不自动套用顶部和执行模块。今日任务、生效中规划沿用所选区间及执行范围，文案不产生额外当天或有效期过滤。

参与度：区间存在客户经理—技能任务及对应排程定义为收到任务；该技能每天最新执行至少一次 is_read=1 的客户经理计参与。不能只取已有执行客户经理作分母。TopN默认5、正整数，排序字段和细节以接口说明与专项需求为准。

关注支行：查看率、接触率严格低于对应基准才诊断；使用未舍入值，不能套用表格±3pp阈值。接触不足优先，返回总命中数及最多4项。

技能分类已有同 source_id 的分类字典关联；旧版“统一null”不代表当前实现。缺失分类仍可空，见验证记录；业务演进依据见待确认页。

## 代码入口

- [Controller](../../../src/main/java/com/cmbchina/adapter/report/web/ReportController.java)
- [顶部与候选项应用服务](../../../src/main/java/com/cmbchina/application/report/service/ReportApplicationServiceImpl.java)
- [规划应用服务](../../../src/main/java/com/cmbchina/application/report/service/PlanCoverageApplicationServiceImpl.java)
- [执行应用服务](../../../src/main/java/com/cmbchina/application/report/service/ExecutionReportApplicationServiceImpl.java)
- [持久化链路](../../../src/main/java/com/cmbchina/infrastructure/report/supportimpl/ReportDataSupportImpl.java)
- [Mapper SQL](../../../src/main/resources/mapper/report/ReportMapper.xml)

完整契约保留在[接口说明](../../report-api.md)。专项依据：[规划覆盖](../../report-plan-coverage-requirements.md)、[来源结构](../../report-plan-source-requirements.md)、[执行视角](../../report-execution-requirements.md)、[执行分析](../../report-execution-analysis-requirements.md)。这些来源的历史状态和冲突不因本轮归档自动改写。
