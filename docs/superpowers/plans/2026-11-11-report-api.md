# 智拓报表接口 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Goal:** 提供联动筛选和顶部六项指标及辅助数据两个只读接口。

**Architecture:** 分批查询、Java 全区间汇总。入口委派应用服务接口，领域支持接口隔离 MyBatis XML 持久化，领域计算保持独立。

**Tech Stack:** Java 8、Spring Boot 2.7.2、MySQL、MyBatis、Lombok、Guava。

**Spec:** docs/knowledge/README.md；业务口径以 docs/report-api-requirements.md 为准。

## Global Constraints

- 分行参数 bbkId、支行参数 orgId，结果 skill_id 关联 scene_id。
- 每次 SQL 最多三表关联，禁止跨表子查询、注解 SQL、JdbcTemplate、select *。
- HTTP 200；Result 统一响应；参数错误 10000，未知异常 99999，沿用 ResponseEnum。
- 不新增前端、建表、写入、鉴权、日志模块或外部配置项。
- 遵守 AGENTS.md：不自动新增或执行测试，不自动提交；验证使用静态检查、编译和架构门禁。下述边界检查为代码及 SQL 审阅，不能宣称替代运行测试。

## Review Focus

- 相同对象跨人员、日期和批次出现时，全区间去重。
- 最新执行没有结果时保留规划、技能、人员和支行统计。
- 基准取消支行、等级、技能，不能复用已缩小的当前结果。
- 点击事件 OR 括号正确，接触空字符串保留、NULL 排除。
- 零分母返回 null，差值从未舍入比率计算。

## 文件与职责

以下 Java 路径均位于 src/main/java/com/cmbchina/。

- adapter/report/web/ReportController.java：两个 GET 入口。
- application/report/api/ReportApplicationService.java：用例契约。
- application/report/api/dto/：ReportFilterRequest、ReportSummaryRequest、ReportFiltersResponse、ReportSummaryResponse、ReportOption、MostUsedSkillResponse，每个类型独立文件。
- application/report/service/ReportApplicationServiceImpl.java：校验及查询、转换编排。
- domain/report/model/：ReportQuery、ReportRelation、ReportExecution、ReportRecommendation、ReportContact、ReportStatistics，每个类型独立文件，保存阶段关系及汇总结果。
- domain/report/support/ReportDataSupport.java：领域查询契约。
- domain/report/service/ReportStatisticsService.java 与 ReportStatisticsServiceImpl.java：汇总契约及实现。
- infrastructure/report/dao/mapper/ReportMapper.java、dao/po/ 下对应阶段 PO、dao/converter/ReportDataConverter.java、supportimpl/ReportDataSupportImpl.java：持久化与边界转换。
- src/main/resources/mapper/report/ReportMapper.xml：所有只读 SQL。
- pom.xml、src/main/resources/application.yml：仅必要依赖及 Mapper 配置。
- docs/report-api.md：请求和响应说明及运行验证限制。

## Task 1：检索选项与持久化关系

**Interfaces:** 应用服务 `ReportFiltersResponse filters(ReportFilterRequest request)`；领域支持 `List<ReportRelation> findRelations(ReportQuery query)`、`boolean matchesOrganization(String bbkId, String orgId)`。ReportQuery 包含 bbkId、orgId、level、skillIds、startDate、endDate。

- [x] 核对真实表字段、现有异常处理及配置，确定 PO 字段类型，沿用已有错误转换。
- [x] pom.xml 补充 Java 8 兼容 MyBatis starter 和 Guava；application.yml 仅配置 Mapper XML 位置及必要映射。
- [x] 实现步骤 1—3 人员、规划、目标、场景、技能及任务关系查询，按最多三表拆分；内部固定批次大小，空键集合直接返回空集合，组合关系按完整键匹配，禁止逐记录查询。
- [x] 实现三个候选集合各自移除自身过滤的查询，等级取实际值；选项按编号去重并排序，名称仅展示。机构匹配独立于技能范围验证。
- [x] 实现 filters 入口与应用契约，参数使用 bbkId、orgId、skillIds；响应 branches、subBranches、skills、levels，前三类为 id/name。
- [x] 审阅候选项无日期、排程或执行依赖；技能空集代表全部，多技能为或；参数错误使用 10000。
- [x] 对本任务新增修改 Java 执行 `bash scripts/java-quality.sh verify` 加精确文件路径；退出码必须为 0。

## Task 2：执行、结果及接触查询

**Interfaces:** ReportDataSupport 增加 `List<ReportExecution> findExecutions(ReportQuery query, List<ReportRelation> relations)`、`List<ReportRecommendation> findRecommendations(List<ReportExecution> executions)`、`List<ReportContact> findContacts(List<ReportRecommendation> recommendations)`。阶段模型保留后续匹配必需的组织、sapId、planId、skillId、jobId、执行日期时间、traceId、isRead、custuid 等字段，不把独立 ID 集合当作完整关系。

- [x] 实现 execute_day 闭区间排程查询，以及任务每天 actual_time 最新 trace 的选择；执行时间不早于规划创建时间，并按 traceId 获取执行记录。
- [x] 实现按 jobId、skillId=scene_id、traceId 精确匹配的成功最新 plan 结果查询；不增加过期、执行状态和 is_final 限制。
- [x] 实现点击与接触查询：trace、customerId、执行当天匹配；条件 `button_click OR (preview_view AND template_type=sub)`；clickId 对应明细，不增加客户一致性或接触日期限制。
- [x] 所有查询分批且不越过三表限制；审阅无结果执行保留、日期首尾及相同时间并列行为，跨批次统一合并。
- [x] 对本任务新增修改 Java 执行精确路径 verify，核对 Mapper XML 参数与 PO 映射。

## Task 3：汇总及 summary 入口

**Interfaces:** 领域服务 `ReportStatistics summarize(List<ReportExecution> executions, List<ReportRecommendation> recommendations, List<ReportContact> contacts)`；应用服务增加 `ReportSummaryResponse summary(ReportSummaryRequest request)`。

- [x] 实现规划、技能、工号和分行/支行组合去重；名单任务按 traceId、方案任务按 traceId/custuid 去重后相加。
- [x] 实现非空推荐客户、查看客户、接触客户集合，接触忽略 NULL 但保留空字符串；查看客户取 isRead=1 执行对应客户。
- [x] 实现最多使用技能及人数，并列按技能编号升序；实现覆盖支行、人均任务和人均客户。数量使用 long，除法使用 BigDecimal、HALF_UP。
- [x] 实现两位百分比、整数人均和零分母 null；基准差值由未舍入分数相减后计算百分点。
- [x] 应用服务独立加载保留日期和 bbkId 的基准范围，通过同一领域算法汇总；实现 summary 请求日期必填、格式和顺序校验，以及统一错误响应。
- [x] 完成设计中的全部响应字段转换；审阅 Review Focus 五项，并逐条对照需求六项指标及辅助口径。
- [x] 对本任务新增修改 Java 执行精确路径 verify。

## Task 4：交付验证和接口说明

- [x] 编写 docs/report-api.md，给出两个接口 bbkId/orgId 参数示例、成功和 10000 错误结构、全部字段及 null 语义。
- [x] 核对 `java -version`、`mvn -version` 与项目 Java 8 配置，不更改指定 JDK；读取门禁脚本确认不运行未经授权的测试。
- [x] 对全部本次新增修改 Java 执行 `bash scripts/java-quality.sh verify` 加精确路径，并执行项目编译及 `bash scripts/backend-gate.sh src/main/java/`。若脚本包含测试，先取得授权或使用其受支持的不运行测试模式，不能绕过静态或架构检查。
- [x] 核对实际改动未触犯 AGENTS.md 禁止项；修正本次问题后仅重跑受影响检查。
- [x] 报告实际命令、范围和退出结果，明确数据库运行时验证及未授权测试未执行情况；依赖、环境或检查阻塞时不宣称开发完成。

## 实施记录

- Ruling：当前目录无 Git 仓库，在授权项目原目录实施，不创建 worktree 或提交；无法提供 Git 差异和提交记录。
- Ruling：遵守项目禁止自动新增测试的要求，执行用户已授权的接口请求验证及质量、编译、架构检查，不新增单元测试。
- Ruling：技能条件在分批任务查询后通过集合匹配，避免用户技能列表形成无界 IN；仍按完整人员/规划/技能关系匹配。
- Ruling：对统一异常处理增加绑定错误消息整理，避免报表日期格式错误将原始输入或内部异常返回给调用方。
- 环境：本机 MySQL zhituo 的 11 张表已存在，人员、任务、执行、结果表均无业务数据。真实数据统计数值未验证，不插入数据。


## 最终验证记录

- 使用 README 指定的 JDK 8（1.8.0_512），Maven 3.9.14。
- `bash scripts/java-quality.sh verify src/main/java/com/cmbchina/adapter/report src/main/java/com/cmbchina/application/report src/main/java/com/cmbchina/domain/report src/main/java/com/cmbchina/infrastructure/report src/main/java/com/cmbchina/adapter/web/advice/GlobalExceptionHandler.java`：退出码 0，0 Checkstyle violations。
- `MAVEN_ARGS='-o -s /tmp/zhituo-maven-settings.xml' bash scripts/backend-gate.sh src/main/java/`：退出码 0；全量 Java 静态及格式检查、clean compile、架构规则检查通过；架构检查 1 项、失败 0。临时 Maven 设置仅用于将依赖仓库统一指向 Central，不修改项目质量规则。
- 使用 `mvn -o -s /tmp/zhituo-maven-settings.xml -Dmaven.repo.local=.tool-cache/m2 spring-boot:run` 启动最终代码，端口 8080。
- `python3 /tmp/zhituo-interface-check.py`：8/8 通过，HTTP 均为 200。验证 filters 空集合、summary 零数量与 null 比率/人均、日期缺失、错误日期不暴露输入、日期倒置、机构不匹配、技能多选及带过滤 summary。参数错误 code=10000。
- 独立代码审阅发现并修复 NULL trace_id 导致基础执行被丢弃的问题；复核确认修复，无剩余 Critical/Important 问题。由于数据库为空，此边界仅完成代码审阅，未进行有数据运行验证。
- 未新增或运行业务单元测试、e2e 测试；项目强制架构检查已执行。未建表或插入数据。跨日、跨批次去重、联动候选项内容及机构基准数值待有数据环境联调，不能据本轮空数据验证宣称这些数值已通过验收。


## 用户授权造数后的补充验证

2026-11-11，用户明确要求“造数完成验证”。在本机 zhituo 库插入可精确清理的211人数据，完成 `python3 scripts/report-validation/verify.py`，退出码0，16/16场景通过。已验证跨日/跨人员/跨技能/跨批次去重、联动选项内容、机构基准、未舍入差值、NULL trace及接触空字符串等此前未验证内容。执行 cleanup.sql、重新装载 seed.sql 后复验全部通过，数据保留供复验。详见 docs/report-api-validation.md。本轮不修改Java。
