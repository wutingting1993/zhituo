# 执行分析接口验证记录

2026-11-12，使用本机 MySQL `zhituo` 现有验证数据及独立随机端口服务验证三个新接口。最终153项检查通过，退出码0，结果success=true；未改Java或数据库数据。

## 文件与实际命令

- [验证脚本](../scripts/execution-validation/verify_analysis.py)
- [最终结果](../scripts/execution-validation/analysis-results.json)
- [接口说明](report-api.md) 第3.10—3.12节

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
mvn -B -Dmaven.repo.local=.tool-cache/m2 spring-boot:run -Dspring-boot.run.arguments=--server.port=0
python3 scripts/execution-validation/verify_analysis.py http://127.0.0.1:63115
python3 -c "import ast,pathlib; ast.parse(pathlib.Path('scripts/execution-validation/verify_analysis.py').read_text()); print('syntax OK')"
git diff --check
```

63115为本轮临时随机端口，复验时须替换。首次沙箱内启动和TCP访问受限，随后通过获批的本机网络执行权限启动服务、连接验证库。未停止或重启已有18082、18083服务。

脚本仅SELECT相关11张表，以Python集合、Fraction精确比率及Decimal舍入独立计算预期值，不调用Java统计方法作为预期结果，不将旧接口响应当作数据库真值。不新增数据，不需要数据库清理；临时服务在验证后停止。

## 已验证范围

| 范围 | 实际结果 |
| --- | --- |
| 执行走势 | 完整12日、单日、无数据日逐日返回，日期升序、四项客户数与率、任务数逐项匹配数据库独立结果 |
| 公共筛选 | 全范围、指定分行、支行+等级+技能组合、无推荐结果技能、单日和空区间六种范围 |
| 查看及点击口径 | 每日最新执行、read映射、insight/phone事件、客户去重及点击日期关系与当前数据库结果一致 |
| 技能分母 | 实际存在2组已分配但未执行的人员技能关系，计入应执行人数，不被执行数据过滤 |
| 技能排名 | 7技能，三种排序分别核对TOP1、TOP2、TOP1000；先精确排序再截取，默认参与人数及TOP5，显式null同默认 |
| 关注支行 | 全范围命中7家，totalCount保持7而仅返回4条；三类诊断均有实际样本，身份、名称、客户数、率、基准、差值、顺序均匹配 |
| 基准与零值 | 取消支行/等级/技能筛选重新计算基准；空分母返回null、空排名和空关注列表正确；未截断超过100%的接触率 |
| 参数错误 | 三接口缺日期、无效日期、日期倒置；技能缺/null filters、topN为0/-1/1.5/超整数范围/非法字符串，以及空/非法sortKey |
| 统一错误包装 | 上述错误HTTP200、code=10000、success=false、data=null，并有错误说明 |

初轮152项通过；补充关注支行名称字段及未执行人群样本断言后复跑，最终以153项结果文件为准。独立只读审阅未发现影响验证结论的重要漏洞；审阅发现的名称字段覆盖缺口已补齐并复跑。

## 限制和检查说明

本轮未修改Java，无需重复上一轮已通过的10文件verify、编译及架构检查。验证脚本通过ast语法检查，文档及改动通过git diff --check；项目未配置Python lint，不能将语法检查称为完整静态检查。

本轮为现有本机验证数据的接口联调，不代表生产规模性能、并发或所有极端日期边界验证；未验证原HTML接入。脚本固定只读本机zhituo，依赖既有manifest对应样本仍存在；缺少样本或接口结果不符会抛出错误，结果不会标记成功。
