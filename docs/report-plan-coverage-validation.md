# 分支行规划覆盖接口自测记录

2026-11-11，在本机 MySQL `zhituo` 和新版本服务 `http://localhost:8081` 完成验证。原有8080服务未替换。

## 改动范围

新增分支行覆盖及规划明细两个 POST 接口、应用用例和请求响应对象；新增规划展示模型及持久化读取，通过已有 ReportDataSupport 执行链路和 ReportStatisticsService 去重统计。原有 summary 的范围和计算逻辑不变。不新增数据库表、依赖、鉴权或前端。

## 验证数据

[seed.sql](../scripts/plan-coverage-validation/seed.sql) 创建7条规划、2个分行、3名人员、2项技能及两天执行，字符串业务标识使用项目 CUuid.nextId() 生成；表自增主键由数据库生成。负责人属于另一分行，用于验证负责人展示不改变统计归属。

数据涵盖三类有效来源、其他来源、NULL及空字符串来源、跨支行跨技能规划、没有执行的规划、没有结果的执行、重复结果、名单任务、方案任务，以及已过期有效期和发布失败状态。主场景预期分行规划3条、两个支行规划数之和4条、客户1户、任务20项、技能2项。

[manifest.json](../scripts/plan-coverage-validation/manifest.json) 保存场景编号映射；[cleanup.sql](../scripts/plan-coverage-validation/cleanup.sql) 按本次精确标识清理，不清表、不删除旧验证数据。已执行清理、重装及复验；最后保留本次数据用于人工核对。因新增验证数据，旧验证脚本的全行固定预期需在清理本次数据后运行。

## 实际结果

- `python3 scripts/plan-coverage-validation/verify.py`：30/30场景通过，真实HTTP响应保存于 [results.json](../scripts/plan-coverage-validation/results.json)。脚本默认连接8081，可通过 `REPORT_BASE_URL` 指定服务地址。
- 临时清理本次数据后，将原有16项验证脚本的服务地址指向8081执行：16/16通过；原脚本未修改，输出另存 [regression-results.json](../scripts/plan-coverage-validation/regression-results.json)。之后重新装载本次数据，30项复验全部通过。
- Java使用JDK `1.8.0_512`、Maven `3.9.14`。`bash scripts/java-quality.sh verify` 指定本次新增及修改的14个Java文件，通过；修改实现后对该实现重新verify，通过。
- `bash scripts/backend-gate.sh src/main/java/`：全量Checkstyle、格式检查、业务编译及架构检查通过，退出码0。架构检查1项，失败0、错误0。
- `verify.py` 使用 `ast.parse` 语法检查通过；项目未配置Python lint。未修改JavaScript或前端。

30项包括：分行跨支行去重、来源映射和排除、覆盖支行数、明细总数一致、汇总客户去重及两类任务、负责人独立归属、无结果保留规划、不过滤发布及有效期、单规划任务去重、来源筛选汇总不变、全部来源恢复、仅展示命中技能、多技能OR、支行下钻、分行保留顶部支行条件、等级按目标人员筛选、首尾日期、空区间及空明细、无命中来源，以及日期、机构、来源和嵌套参数校验。

## 复验与清理

```bash
python3 scripts/plan-coverage-validation/verify.py
# 输入本机数据库密码，仅在不再需要本次数据时清理
mysql -h127.0.0.1 -uroot -p --default-character-set=utf8mb4 zhituo < scripts/plan-coverage-validation/cleanup.sql
# 重装前应先清理同一批数据
mysql -h127.0.0.1 -uroot -p --default-character-set=utf8mb4 zhituo < scripts/plan-coverage-validation/seed.sql
```

本轮验证为本机功能验证，不代表生产数据规模下的性能结论。

## 最终交付核对

只读代码审查未发现可确认的新增功能缺陷。最终DTO补齐项目约定的构建注解后，重新执行verify及全量backend gate，退出码均为0；8081服务重启加载最终编译结果，30项HTTP自测再次全部通过。

本次新增Java文件：

- `application/report/api/PlanCoverageApplicationService.java`
- `application/report/api/dto/PlanCoverageResponse.java`
- `application/report/api/dto/PlanCoverageRow.java`
- `application/report/api/dto/PlanDetailsRequest.java`
- `application/report/api/dto/PlanDetailsResponse.java`
- `application/report/api/dto/PlanDetailRow.java`
- `application/report/service/PlanCoverageApplicationServiceImpl.java`
- `domain/report/model/ReportPlan.java`
- `infrastructure/report/dao/po/ReportPlanPo.java`

本次修改Java文件：`ReportController.java`、`ReportDataSupport.java`、`ReportDataConverter.java`、`ReportMapper.java`、`ReportDataSupportImpl.java`。另修改 `ReportMapper.xml`、README、接口说明和规划覆盖需求文档；新增本验证记录及 `scripts/plan-coverage-validation/` 下的造数、清理、验证和结果文件。不处理工作区原有其他改动。

初次14个文件的质量检查命令如下，修改实现后单独重复检查该实现；最终DTO及模型调整后重复检查对应文件：

```bash
bash scripts/java-quality.sh verify \
  src/main/java/com/cmbchina/application/report/service/PlanCoverageApplicationServiceImpl.java \
  src/main/java/com/cmbchina/application/report/api/PlanCoverageApplicationService.java \
  src/main/java/com/cmbchina/application/report/api/dto/Plan*.java \
  src/main/java/com/cmbchina/domain/report/model/ReportPlan.java \
  src/main/java/com/cmbchina/domain/report/support/ReportDataSupport.java \
  src/main/java/com/cmbchina/infrastructure/report/dao/po/ReportPlanPo.java \
  src/main/java/com/cmbchina/infrastructure/report/dao/mapper/ReportMapper.java \
  src/main/java/com/cmbchina/infrastructure/report/dao/converter/ReportDataConverter.java \
  src/main/java/com/cmbchina/infrastructure/report/supportimpl/ReportDataSupportImpl.java \
  src/main/java/com/cmbchina/adapter/report/web/ReportController.java
bash scripts/backend-gate.sh src/main/java/
python3 scripts/plan-coverage-validation/verify.py
git diff --check
```
