# 规划来源结构接口验证记录

2026-11-12，通过本机新版本服务 `http://localhost:18082` 和现有 MySQL 数据执行真实 HTTP 验证。未修改数据库数据，未修改业务代码。

## 命令与结果

`REPORT_BASE_URL=http://localhost:18082 python3 scripts/plan-source-validation/verify.py`：退出码0，13/13通过。

覆盖：全范围与覆盖接口结果一致、指定分行、支行、等级、单技能、多技能OR、首尾单日、无数据，以及缺日期、日期倒置、机构不匹配、非法日期。所有响应HTTP为200，参数错误业务码10000。

指定分行的独立固定预期为规划3条、三类来源各1条，同时验证跨支行与跨日期去重。各响应只包含分行来源字段，不含支行明细；来源之和等于规划总数。

脚本以覆盖接口作为筛选一致性对照；并非所有统计口径都有独立预期。结果保存于 [results.json](../scripts/plan-source-validation/results.json)，脚本见 [verify.py](../scripts/plan-source-validation/verify.py)。Python ast.parse语法检查通过；项目未配置Python lint，未将语法检查视为静态检查通过。

## 未验证范围

首次验证日期范围只有1个符合条件的分行；下述补验已覆盖超过5个分行的截取及并列排序。未进行生产规模性能验证。

启动时8082端口已占用，改用18082，未终止已有服务。本次验证服务使用18082端口，未确认停止。

## 排名边界补验

新增独立日期2026-09-15的7个分行、29条规划，规划数量分别为7、6、5、4、3、3、1；业务字符串编号使用项目CUuid.nextId()生成，未新增数据库结构或修改业务代码。

`python3 scripts/plan-source-validation/verify_boundaries.py`：退出码0，10/10通过。使用独立预期核对7取5、数量降序、第5/6名并列编号升序、三类来源数量、重复3次排序稳定、筛选原未入榜分行及支行、筛选无结果。

种子、编号映射、精确清理和结果分别保存于scripts/plan-source-validation/下的boundary-seed.sql、boundary-manifest.json、boundary-cleanup.sql、boundary-results.json。prepare.py用于生成数据文件，重新生成前需准备由CUuid生成的编号文件，不应在未清理旧批次前覆盖映射。

已执行boundary-cleanup.sql，退出码0，仅按本批精确编号删除；随后原13项HTTP验证再次13/13通过。合计23项场景通过。新增两个Python脚本ast.parse语法检查通过，项目无Python lint，未宣称静态检查通过。

补验开始时18082不可用，重新启动后完成测试；服务保留供本机复验。

## 可配置topN验证

接口新增可选topN，省略或null默认5，正整数，不设额外上限。18083新版本服务执行 `python3 scripts/plan-source-validation/verify_topn.py`，10/10通过：默认、省略、1、3、6、7、超过现有数量、0、负数和非法字符串（默认两项分别为省略和null）。结果保存于topn-results.json；重装的边界数据已执行精确清理，退出码0。新增Python脚本语法检查通过，项目未配置Python lint。

本次修改4个Java文件的java-quality verify退出码0；JDK17执行Maven compile通过（目标Java8），architecture-check通过。接口文档及需求同步为Top N。
