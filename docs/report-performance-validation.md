# 排程前置筛选结果一致性验证

2026-11-12，使用本机 MySQL `zhituo` 现有验证数据，完成优化前后服务的真实 HTTP 响应对比。未插入、修改或删除数据库记录。

## 对照版本

在 `/private/tmp/zhituo-result-compare-vsjmwqc0` 创建两个源码副本，不替换项目或现有服务。

- after：本次优化后的 `src/main` 与 `pom.xml`。
- before：从同一副本撤销本次候选裁剪逻辑：恢复全量 targets 读取、移除 targets 的规划 ID 条件和 jobs 的排程组合条件；恢复按目标批次加载任务。保留未调用的新增 Mapper 声明及方法参数，不参与查询。它是重建的优化前查询逻辑对照版本，不是 Git 历史构建产物。
- 两个副本其他业务源码、配置及依赖保持一致；使用 README 指定的本机 JDK 8 分别编译、启动，端口为18091、18092，共用同一本机验证库。

## 命令与结果

```bash
python3 scripts/report-performance-validation/compare.py \
  http://127.0.0.1:18091 http://127.0.0.1:18092 \
  scripts/report-performance-validation/results.json
```

退出码0，268组对比通过。直接比较完整解析后的 JSON 和 HTTP 状态，不忽略字段、null、数组顺序或错误消息。JSON 对象键的序列化顺序不参与比较。

| 接口（统一前缀 `/api/v1/reports/`，POST） | 通过数 |
| --- | ---: |
| summary | 35 |
| execution-overview | 35 |
| plan-coverage | 35 |
| plan-sources | 35 |
| execution-branches | 31 |
| execution-branches/managers | 31 |
| execution-branches/skills | 31 |
| plan-coverage/details | 31 |
| filters | 4 |

其中248组为统计接口正常请求，16组为日期倒置或缺少单个日期的参数错误，4组为filters独立查询。覆盖两套既有验证数据：全行、分行、支行、等级、单技能、多技能OR、组合条件、首尾单日、空日期、未知技能、机构不匹配、空及null技能数组、NULL trace、无执行和205人跨批次。

额外断言报表B1的规划3、任务7、客户3；批次场景任务205、客户1；NULL trace场景规划1、任务0；无执行场景规划及任务0；规划覆盖数据B1规划3。避免只比较空响应。既有数据还包含早于规划创建时间的执行、旧执行、无推荐结果和无排程日执行；本次通过包含这些数据的请求核对整体响应，没有单独新增边界造数。

## 已发现的差异与限制

首次同时省略开始和结束日期时，两个服务的错误文案字段顺序不同：`endDate…；startDate…` 与 `startDate…；endDate…`；HTTP状态、错误码及其他字段相同。该请求在进入数据查询前已被参数校验拒绝，与本次排程筛选逻辑无关；没有修改异常处理代码。原始响应保存在 `scripts/report-performance-validation/initial-results.json`。最终套件改为分别验证缺少单个开始日期，仍严格比较完整消息，不对消息排序或忽略。

本轮证明上述现有数据和请求下统计响应一致，不代表任意生产数据均已验证。未覆盖超过200个不同排程组合的新增批次边界、相同时间执行并列及生产规模性能。本轮不量化加速效果。

Python脚本通过 `ast.parse` 语法检查；项目没有配置Python lint，语法检查不作为完整静态检查结论。本轮没有修改Java业务代码。两个临时服务已停止，临时源码副本保留便于复核；复跑需要重新启动对照服务。
