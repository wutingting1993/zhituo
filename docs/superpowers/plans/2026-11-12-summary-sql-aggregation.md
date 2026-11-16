# 顶部统计 SQL 聚合实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** 将POST /api/v1/reports/summary的明细读取与Java去重改为数据库聚合，接口契约保持不变。

**Architecture:** Mapper XML通过一条分阶段组织的大SQL构建统计数据集，使用显式INNER JOIN和LEFT JOIN以及聚合；数据库只返回一行统计结果。Java继续计算比率、差值和响应。可以使用MySQL 8非递归CTE表达中间查询，CTE不是临时业务表，也不将阶段数据返回Java。

**Tech Stack:** 当前Java 8、Spring Boot、MyBatis XML、MySQL 8；不新增依赖。

**Spec:** 本会话确认的大SQL、标准JOIN、仅summary改造；业务和字段参考 ../../report-api.md 及 ../../knowledge/shared/report-rules.md，实际兼容基线为当前实现。

## Global Constraints

- 用户明确授权本任务使用多表大SQL，作为超过三表JOIN、跨表子查询限制的任务级例外；不修改全局规范。
- 所有关联使用显式JOIN ... ON，不使用逗号连接、SELECT *、永久表、临时表、视图、存储过程或新增索引。
- 其他接口不切换查询路径，不删除它们仍使用的明细查询及统计服务。
- 接口文档同步解释实现变化，参数、字段、空值和错误码不变。
- 不新增或执行未授权单元、e2e或性能测试；必须执行适用Java静态、编译与架构检查。

## Review Focus

- 同日多次执行仅按actual_time选最新；同时间并列允许任选一条，不能以新规则改为优先成功。
- 最新执行trace_id为空时仍保留执行维度；同trace回查的同任务同日记录及方案查看状态按现实现处理。
- 无执行的排程参与应执行/待执行，但不参与已执行规划、技能、客户经理及覆盖支行数量。
- 一对多结果和点击关联不能放大任务、客户等计数；空客户UID、NULL接触UID边界不变。
- 分支行组合和跨日期客户必须整体去重；零结果仍返回完整零统计行和可空最多技能。

## Task 1：SQL与聚合边界

**Files:** 修改 src/main/resources/mapper/report/ReportMapper.xml、src/main/java/com/cmbchina/infrastructure/report/dao/mapper/ReportMapper.java、src/main/java/com/cmbchina/domain/report/support/ReportDataSupport.java、src/main/java/com/cmbchina/infrastructure/report/supportimpl/ReportDataSupportImpl.java；新增 src/main/java/com/cmbchina/infrastructure/report/dao/po/ReportSummaryPo.java；按需修改现有ReportDataConverter.java。

**Interfaces:** Mapper新增 ReportSummaryPo summary(@Param("query") ReportQuery query)；领域支持新增 ReportStatistics summarize(ReportQuery query)。PO含ReportStatistics全部标量字段和最多技能的id/name/userCount，转换后不得跨持久化边界暴露PO。

- [x] 创建聚合PO，沿用当前项目PO注解风格。
- [x] 在XML定义查询阶段：有效规划目标及客户经理、场景技能任务、范围内排程、每天最新执行、同链路回查执行、成功结果、有效点击接触。
- [x] 应执行任务按客户经理/规划/技能/日期/任务组合计数；已执行和失败按相同键计数，待执行为差值。失败沿用最新执行status不等于success。
- [x] 已执行维度独立聚合；成功方案按trace_id/custuid非空组合去重，推荐及查看客户按UID整体去重，接触客户按UID去重并保留空字符串；不要将所有数量放在膨胀后的单一宽表直接COUNT。
- [x] 最多技能按客户经理工号去重人数降序、技能编号升序，名称取值需对齐原行为或明确不可稳定的并列名称边界。
- [x] 使用单行基础聚合结果LEFT JOIN最多技能，确保空数据返回一行；比较双方相同NULL组合语义，避免COUNT(DISTINCT多列)忽略NULL而改变现有Set统计。
- [x] 实现支持层转换及Mapper契约，核对上述五项Review Focus；本阶段人工SQL审查，不执行数据库查询。

## Task 2：切换summary与交付检查

**Files:** 修改 src/main/java/com/cmbchina/application/report/service/ReportApplicationServiceImpl.java、docs/report-api.md。

**Interfaces:** 保留原summary请求响应和response(current, benchmark)，将私有summarize委托到data.summarize；仅删除本改动造成的未使用依赖。

- [x] 当前范围和机构基准使用同一SQL契约，保留日期与分行、去掉其他筛选的基准规则；本轮不增加缓存或其他接口联动。
- [x] 同步接口说明中的数据处理方式及性能未验证限制，不声称已获得提速。
- [x] 对本次Java文件执行 bash scripts/java-quality.sh verify <具体文件清单>，执行 bash scripts/backend-gate.sh src/main/java/，必要编译使用README指定JDK。
- [x] 检查Mapper XML格式、别名、字段转换和文档链接，人工对照Review Focus五类情形及SQL去重范围；人工推演不得记为运行验证。
- [x] 交付实际检查结果，明确数据库SQL运行一致性、执行计划和生产规模性能尚未验证。后续运行测试需另获授权，不因静态通过宣称SQL运行正确或性能达标。

## 自检

保留现有接口与业务口径；Java不加载summary执行/推荐/客户行为明细。大SQL可能仍在MySQL内部物化、排序并使用磁盘，数据库成本需后续EXPLAIN与授权测试测量，不能承诺性能倍数。客户端内存减少不等于数据库内存零占用。

## 执行补充

用户追加授权开发后进行非性能测试，覆盖计划原先不执行功能测试的限制。实际51项只读HTTP功能对照通过，见../../report-summary-sql-validation.md。SQL运行一致性已在本机现有数据范围验证；生产性能和未造数边界仍未验证。
