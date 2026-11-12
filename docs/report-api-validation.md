# 报表接口造数验证记录

2026-11-11，在本机 MySQL `zhituo` 和 `http://localhost:8080` 完成验证。

## 数据与文件

本次准备 211 位客户经理，覆盖 4 个分行、多支行、5 个技能及两天执行。包含 205 人使用同一技能、推荐同一客户的分批场景。所有字符串业务标识由项目 `CUuid.nextId()` 生成；表自增主键沿用现有数据库规则。数据仅用于本机接口验证，不改变建表结构或后端业务代码。

- [seed.sql](../scripts/report-validation/seed.sql)：插入数据，事务提交；不覆盖已有记录。
- [cleanup.sql](../scripts/report-validation/cleanup.sql)：按本次生成的精确标识删除数据，不使用清表操作。
- [manifest.json](../scripts/report-validation/manifest.json)：场景名称与实际编号映射，如 B1、O1、S1。
- [verify.py](../scripts/report-validation/verify.py)：通过 HTTP 请求核对预先计算的结果，遇到不符立即失败。
- [results.json](../scripts/report-validation/results.json)：本轮通过场景及接口实际返回。

验证数据当前保留在本机数据库，便于人工复验。重复装载前先执行 cleanup.sql；禁止在生产环境执行。

## 验证结果

实际命令 `python3 scripts/report-validation/verify.py` 退出码 0，**16/16 场景通过**。

| 场景 | 已核对结果 |
| --- | --- |
| B1 全区间 | 规划3、技能3、任务7、客户3、查看率100%、接触率133.33% |
| 支行、等级、技能组合筛选 | 规划2、技能1、任务5、客户3；基准仍为分行整体 |
| 多技能 OR、技能同名 | 技能按编号计2项，客户跨技能去重 |
| NULL trace | 保留规划、技能及人员统计，任务与推荐客户为0 |
| 无推荐结果执行 | B1规划和人员分母仍包含该执行，结果数量不增加 |
| 未查看技能 | 查看率0，较基准差值为负数 |
| 首尾日期 | 两个单日查询分别匹配对应排程和最新执行 |
| 只有候选任务、没有执行 | filters 保留候选，summary 不计基础指标 |
| 205人跨批次 | 规划1、技能1、任务205、推荐客户1，最后批次的查看生效 |
| 全行汇总 | 规划7、技能4、任务222、客户14；按分行/支行组合覆盖6个支行 |
| 差值未舍入计算 | 当前16.67%、基准44.44%，差值为-27.78个百分点 |
| 技能人数并列 | 按技能编号升序取第一项 |
| 未选分行的基准 | 保留日期，使用全行基准，不保留支行条件 |
| 三类联动选项 | 分别移除自身筛选，并保留其他两类条件 |
| 空日期区间 | 数量0，率、人均及最多技能为null |

主场景还包含重复结果、旧执行、失败结果、非最新成功结果、错误 result_type、错误 job/skill、已删除规划/任务、其他来源任务、多技能整字段不匹配、早于规划创建时间的执行、无排程日期的执行、过期结果和已失效规划日期，实际汇总与预期一致。

接触数据覆盖 button_click 不受模板类型限制、preview_view 仅 sub、重复客户、空字符串、NULL、错误客户、错误事件、非执行日点击及不限制接触日期和按钮类型。接触客户可以不同于推荐客户；实际接触率133.33%，未截断为100%。

## 重复验证和清理

在项目根目录执行，按本机账户提供数据库密码：

```bash
mysql -h127.0.0.1 -uroot -p zhituo < scripts/report-validation/seed.sql
python3 scripts/report-validation/verify.py
# 仅在不再需要验证数据时清理
mysql -h127.0.0.1 -uroot -p zhituo < scripts/report-validation/cleanup.sql
```

已实际执行一次清理、重新装载和16项复验。清理后人员、任务、执行、结果表恢复0条；重新装载后16项全部通过，最后保留数据。

Python 验证脚本通过 `ast.parse` 语法检查。项目未配置 Python lint；本轮未改 Java，因此不重复运行 Java 质量门禁。本轮验证不代表生产数据规模下的性能结论。

## POST JSON body 改造复验

两个接口现仅接受 POST，参数从 JSON body 读取。执行 `python3 scripts/report-validation/verify.py`，16/16 数据场景再次通过。另执行10项协议校验全部通过：filters 空对象、两个 GET 请求拒绝、filters 缺 body、summary 缺日期、非法 JSON、非法日期、日期倒置、仅查询参数不能提供日期、查询参数不能覆盖 body 的分行。

全部响应 HTTP 为200；参数错误 code=10000，GET 方法不支持 code=10003。新增修改 Java 的 `java-quality.sh verify` 及全量 `backend-gate.sh src/main/java/` 退出码均为0；Python脚本语法检查及 `git diff --check` 通过。
