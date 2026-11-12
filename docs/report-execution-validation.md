# 执行视角接口联调记录

2026-11-11，在本机 MySQL `zhituo` 与临时随机端口服务完成联调。没有修改后端业务代码或数据库结构。

## 文件与命令

- [验证脚本](../scripts/execution-validation/verify.py)：复用此前报表验证数据（依赖 `scripts/report-validation/manifest.json` 对应数据仍存在），通过 POST JSON 请求验证四个执行视角接口及 summary。
- [验证结果](../scripts/execution-validation/results.json)：通过检查名称、数量及临时数据清理结果。

实际执行：

```bash
python3 scripts/execution-validation/verify.py http://127.0.0.1:57264
```

退出码0，70项检查全部通过。该端口为本轮临时端口，复验时传入实际服务地址。

脚本通过 `ast.parse` 语法检查。项目没有已配置的 Python lint，未将语法检查视为完整静态检查。本轮仅新增验证脚本与文档，未改 Java，不重复运行上一轮已通过的 Java 质量门禁。

## 验证范围

| 范围 | 结果 |
| --- | --- |
| 已知数据 | B1任务7、推荐客户3、查看客户3、接触客户4，与既有口径一致 |
| 顶部一致性 | 全范围、技能筛选、机构/等级/技能组合、无推荐结果、空区间，与 summary 的任务、客户、查看率、接触率及人均值一致 |
| 四项比率 | 查看、洞察、电访、接触均以推荐客户数为分母，零分母返回 null |
| 点击行为 | 洞察insight、电访phone；重复点击去重，错误日期、错误客户、其他按钮值、非按钮事件不计入 |
| 最大流失环节 | 按最小比率选中电访→接触，数量差-3、流失率-300%，保留负值；全部零分母返回 null |
| 支行与基准 | 支行行数据与独立支行概览一致，机构基准与全分行概览一致 |
| 两类下钻 | 汇总与支行一致，任务倒序及编号次序正确，技能筛选保留；分类名称字段显式返回 null |
| 支行排序 | 七个字段分别升序、降序，共14项检查 |
| 空数据 | 两类下钻均返回空列表，仍保留合法机构名称 |
| 错误参数 | 缺日期、日期倒置、缺filters、非法排序字段/方向、缺下钻机构、超出顶部机构范围、分支行不匹配，均HTTP200且code=10000、success=false |

## 数据清理与限制

复用的人员、任务、执行、推荐和原有点击数据保持不变。本轮仅临时插入9条点击，沿用数据库自增主键；finally按捕获的精确主键及专用标记清理。清理后点击表总数恢复原值，`results.json` 中 `cleanupVerified=true`。复验只允许在本机验证库执行，不应对生产库执行。

本轮不代表生产规模性能验证；非零流失率并列、精确±3个百分点阈值和混合null排序已补充专项验证，见下节。前一轮已完成静态检查、编译、架构检查及启动验证。

## 专项边界复验

实际执行：

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
python3 scripts/execution-validation/verify_boundaries.py http://127.0.0.1:57831
```

退出码0，新增27项检查全部通过；与上一轮70项合计97项已通过检查。本轮端口为临时随机端口，复验时应替换为实际服务地址。

- [边界验证脚本](../scripts/execution-validation/verify_boundaries.py)
- [边界验证结果](../scripts/execution-validation/boundary-results.json)
- [临时数据标识](../scripts/execution-validation/boundary-manifest.json)
- [精确清理SQL](../scripts/execution-validation/boundary-cleanup.sql)

独立临时分行包含4家支行：两家各推荐100户，查看、洞察、电访、接触客户分别为53和47户，机构基准50%；另外两家有执行但无推荐结果。另一个日期设置8→4→2→1客户，三段流失率均50%。

| 场景 | 验证结果 |
| --- | --- |
| 四项比率恰好+3个百分点 | 全部high |
| 四项比率恰好-3个百分点 | 全部low |
| 零分母比率 | 无评价标识，返回null |
| 四项比率混合null升序、降序 | 有值按方向排序，null始终最后；两个null按机构编号升序 |
| 非零流失率并列 | 三段均50%，选查看→洞察，客户差4 |
| 查看率差值在阈值以内 | +2.5/-2.5个百分点均normal |
| 查看率差值超过阈值 | +3.5为high，-3.5为low |

业务标识通过项目 `CUuid.nextId()` 生成，自增主键沿用表规则。finally按本次精确业务标识及点击主键清理，未改原有验证数据；涉及10张表的记录数均恢复原值，结果文件 `cleanupVerified=true`。临时服务已停止。

边界脚本通过Python语法检查，项目未配置Python lint；本轮未改Java，不重复执行Java质量门禁。本轮仍不覆盖生产规模性能，以及差值舍入到±3但原始值尚未达到阈值的极近边界造数。

## 技能分类名称复验（2026-11-12）

用户补充 `swe_marketplace_categories` 结构并明确允许在本机 `zhituo` 验证库建表。已执行 [分类建表SQL](../database/skill-categories.sql)，补充主键中文注释，不保留历史自增起点。该表保留，未对其他数据库自动建表。

分类名称按技能 `category_id` 关联分类 `id`，并要求相同 `source_id`。单独分批查询，不增加现有三表任务SQL的关联数量，不改变技能及执行统计范围。

实际执行：

```bash
MYSQL_HOST=127.0.0.1 MYSQL_PORT=3306 MYSQL_DATABASE=zhituo python3 scripts/execution-validation/verify_categories.py http://127.0.0.1:56217
MYSQL_HOST=127.0.0.1 MYSQL_PORT=3306 MYSQL_DATABASE=zhituo python3 scripts/execution-validation/verify.py http://127.0.0.1:56217
```

两条命令均退出码0：新增9项分类检查、原有70项执行视角回归均通过。

- [分类验证脚本](../scripts/execution-validation/verify_categories.py)
- [分类验证结果](../scripts/execution-validation/category-results.json)

覆盖同入口分类名称、branch_visible不作为报表过滤条件、入口不一致、未配置分类、分类记录缺失、空技能列表，以及顶部汇总、技能汇总和行指标不受分类变化影响。

临时两条分类已清理，验证技能的原始 `category_id` 及 `updated_at` 已恢复；分类表记录数恢复验证前数量。回归脚本9条临时点击已清理。临时服务已停止。

本次6个新增或修改Java文件执行 `bash scripts/java-quality.sh verify $(cat /private/tmp/zhituo-category-files)` 与 `bash scripts/backend-gate.sh $(cat /private/tmp/zhituo-category-files)` 均通过，后者包括静态、格式、编译及架构检查。验证脚本通过Python语法检查；项目没有已配置Python lint，不将语法检查等同于完整静态检查。
