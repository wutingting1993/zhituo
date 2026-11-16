# 执行分析接口实施计划

依据已批准设计及后续“开始实施”指令，实施服务端三个模块，原HTML不在本次服务端改动范围。

## 约束与实施决策

- 保留当前既有改动，不清理、重置、提交或推送。
- 用户AGENTS禁止未经授权新增或运行测试；因此使用精确范围verify、编译、架构检查，明确不声称行为测试通过。
- 用户已确认topN可选，省略或null默认5；在当前目录继续，保留已有改动，不创建新工作树。
- 已核实findRelations使用scheduledSkills按区间筛选规划技能，再匹配目标人员及有效任务，未依赖执行，符合已确认应执行分母，可直接复用，不新增SQL。

## 文件及任务

- [x] 在现有ExecutionReportApplicationService及实现中增加trend、participation、attention方法，在ReportController仅添加委派路由。
- [x] 新增ExecutionTrendPoint/ExecutionTrendResponse DTO：POST /execution-trend接收ReportSummaryRequest，返回dailyPoints，逐日完整覆盖，复用已有四率与客户数统计。
- [x] 新增SkillParticipationRequest/SkillParticipationRow/SkillParticipationResponse DTO：POST /skill-participation接收filters、topN、sortKey；任务关系按技能/人员去重得到分母，最新执行read归属相同人员技能得到分子，三种降序和稳定技能编号排序，率精确比较，截取前N条。
- [x] 新增AttentionBranchRow/AttentionBranchesResponse DTO：POST /attention-branches接收ReportSummaryRequest，独立加载基准，按未舍入率判断低于均值，接触不足优先，再查看不足；相同阶段按率升序、推荐数降序、分支行编号升序；返回全部命中数量及前四项。零推荐不入榜。
- [x] 更新docs/report-api.md及需求文档，覆盖路径、方法、请求响应字段、类型、必填/空值、错误、口径与示例，不更改既有接口。
- [x] 用项目可用且兼容工具的JDK，执行新增修改Java精确路径verify、backend-gate；工具实际失败则排查环境，不绕过检查。不新增依赖和测试。
- [x] 执行一次独立审阅，修复本次重要问题后重跑受影响检查；报告实际通过及未验证范围。

## 核对重点

单日/超过七日完整日期；客户跨天去重不能相加；无分母null；接触率不截断；技能包含无执行人员、无成功结果的read执行可参与；排序先于TOPN；关注差值支行减基准、比较不使用舍入值；总数在TOP4之前；基准不携带支行/等级/技能。

## 执行与验证记录

- 三个接口、七个新增DTO、接口说明及需求同步已实施。
- 使用JDK17运行质量工具及Maven，编译仍沿用pom的Java8目标版本，未修改技术栈配置。
- 对本次10个新增/修改Java文件执行 `bash scripts/java-quality.sh verify <精确文件路径>`，退出0、0个Checkstyle违规；存在工具提示的作者/日期Javadoc警告，未放宽检查配置。
- 同一10文件范围执行 `bash scripts/backend-gate.sh <精确文件路径>`，退出0。包含check、format-check、项目clean compile及架构检查（1项、0失败）；未运行业务单元/e2e或数据库行为测试。
- 10文件为ReportController、ExecutionReportApplicationService、ExecutionReportApplicationServiceImpl及七个新增DTO：ExecutionTrendPoint、ExecutionTrendResponse、SkillParticipationRequest、SkillParticipationRow、SkillParticipationResponse、AttentionBranchRow、AttentionBranchesResponse。
- Ruling: 按用户明确指示在当前目录实施并保留既有改动；本次不提交、不推送。按AGENTS约束不新增或运行业务测试，不能将编译或审阅视为运行时口径验证。
- 原HTML未修改或接入，数据库查询无新增，数据库与HTTP实际返回未验证。
- 后续用户授权接口验证：新增只读验证脚本，153项HTTP及独立数据库核对通过，覆盖2组未执行人员技能关系、7技能TOPN及7家关注支行；上述运行未验证状态已由 [验证记录](../../report-execution-analysis-validation.md) 更新。原HTML仍未接入。
- 独立只读代码审阅完成：未发现有证据的重要问题，未遗留审阅修复项。

实际验证命令（均退出0；JAVA_HOME为本机JDK17路径，PATH优先使用该JDK）：

```bash
bash scripts/java-quality.sh verify src/main/java/com/cmbchina/adapter/report/web/ReportController.java src/main/java/com/cmbchina/application/report/api/ExecutionReportApplicationService.java src/main/java/com/cmbchina/application/report/service/ExecutionReportApplicationServiceImpl.java src/main/java/com/cmbchina/application/report/api/dto/ExecutionTrendPoint.java src/main/java/com/cmbchina/application/report/api/dto/ExecutionTrendResponse.java src/main/java/com/cmbchina/application/report/api/dto/SkillParticipationRequest.java src/main/java/com/cmbchina/application/report/api/dto/SkillParticipationRow.java src/main/java/com/cmbchina/application/report/api/dto/SkillParticipationResponse.java src/main/java/com/cmbchina/application/report/api/dto/AttentionBranchRow.java src/main/java/com/cmbchina/application/report/api/dto/AttentionBranchesResponse.java
bash scripts/backend-gate.sh src/main/java/com/cmbchina/adapter/report/web/ReportController.java src/main/java/com/cmbchina/application/report/api/ExecutionReportApplicationService.java src/main/java/com/cmbchina/application/report/service/ExecutionReportApplicationServiceImpl.java src/main/java/com/cmbchina/application/report/api/dto/ExecutionTrendPoint.java src/main/java/com/cmbchina/application/report/api/dto/ExecutionTrendResponse.java src/main/java/com/cmbchina/application/report/api/dto/SkillParticipationRequest.java src/main/java/com/cmbchina/application/report/api/dto/SkillParticipationRow.java src/main/java/com/cmbchina/application/report/api/dto/SkillParticipationResponse.java src/main/java/com/cmbchina/application/report/api/dto/AttentionBranchRow.java src/main/java/com/cmbchina/application/report/api/dto/AttentionBranchesResponse.java
git diff --check
```
