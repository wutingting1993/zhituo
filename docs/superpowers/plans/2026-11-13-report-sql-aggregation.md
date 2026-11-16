# 其余报表接口 SQL 聚合实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** 将除已改造summary及候选项filters外的10个报表统计接口切换为数据库聚合，不向Java加载客户级推荐或行为明细。

**Architecture:** 沿用summary的显式JOIN与非递归CTE，按接口实际维度在SQL中整体去重。Java仅接收总体或分组汇总、必要展示选项，保留比率、精确排序、诊断、树形响应及逐日补零编排。不使用临时表，不新增业务依赖。

**Tech Stack:** Java 8、MyBatis XML、MySQL 8、现有项目质量工具。

**Spec:** 本会话已确认的SQL聚合及标准JOIN方案；接口和边界见 ../../report-api.md ，当前代码为兼容基线。

## Global Constraints

- 用户授权本次扩展SQL改造使用多表JOIN及跨表查询，但不修改全局规范。
- 不改参数、错误码、响应字段、统计口径；任何SQL难以保真的边界先明确处理依据。
- filters已是候选结果查询，不扩展改造；summary保持现有聚合路径。
- 不采用逐分组调用summary的N+1替代方案，也不将结果封装JSON当作客户明细返回。
- 延续用户授权的功能与回归测试，不进行性能测试；不主动造生产规模数据或增加索引。
- 不自动迁移正式长期知识，不自动提交Git；同步当前接口文档和功能验证记录。

## Review Focus

- 机构、客户经理、技能、规划、日期各维度独立去重，汇总不能累加分组客户数。
- 待执行记录在执行视角可形成分组；规划模块先排除无执行记录，保持当前代码行为。
- 参与度分母来自具有区间排程的任务关系，不能仅保留执行或成功结果。
- 规划来源标签只筛选明细，三类来源限制应用于整个规划模块，不传播到执行模块。
- 精确率值排序、null最后、分行支行组合键、日期补零、并列规则及关注totalCount截取前计数均不变。

## Task 1：数据库分组汇总契约

**Files:** ReportMapper.xml、ReportMapper.java、ReportDataSupport.java、ReportDataSupportImpl.java、ReportDataConverter.java；新增必要的聚合PO及领域汇总行模型，均置于现有report分层目录。

- [x] 将summary公共关系、排程、执行选择等SQL片段提取为MyBatis sql/include，避免复制不同口径；现有summary功能保持。
- [x] 增加整体及按日期、分支行组合、客户经理、技能、规划分组的聚合查询，只允许固定枚举维度；不使用客户端字符串直接拼接SQL。
- [x] 汇总包含现有任务计数、客户数量及洞察、电访数量，均在数据库内完成去重，使用二进制标识语义；返回维度编号和必要显示信息。
- [x] 规划模块单独保留三来源限定及只统计实际执行范围；提供分行/支行覆盖与来源计数，不读取无关客户行为。
- [x] 提供规划/客户经理对应的技能展示选项查询，规模为维度关系，不携带客户UID；技能分类沿用现有字典查询。
- [x] 参与度单独聚合有任务关系的客户经理分母与已查看分子，不读取客户结果。
- [x] 核对所有聚合均没有因一对多JOIN放大数量；XML解析及修改Java文件静态检查通过。

## Task 2：执行视角7个接口

**Files:** ExecutionReportApplicationServiceImpl.java及必要的现有模型转换位置。

- [x] overview改用整体汇总；保留最大流失环节取最小值、负数和零分母规则。
- [x] trend改用按排程执行日期汇总，Java按日期补齐零记录和null率值。
- [x] branches与attention改用分支行组合汇总，并独立取得机构基准；保持精确比较和关注totalCount。
- [x] managers与skills将下钻范围直接传入SQL，整体汇总与各维度行独立计算；保留技能选项及分类名称。
- [x] participation改用SQL分子分母汇总，保留三个排序选项及topN，不能按展示舍入值排序。
- [x] 删除本次切换造成的本应用服务内失效明细加载/分组方法及依赖，不删除尚有引用的跨模块对象。

## Task 3：规划视角3个接口

**Files:** PlanCoverageApplicationServiceImpl.java。

- [x] coverage使用分行、支行各自去重汇总组装树，不能累加支行规划数。
- [x] sources使用分行来源聚合排序并截取topN，筛选先于排名。
- [x] details直接在数据库限定下钻范围；来源切换只影响规划列表，总体、来源计数不变。保留规划负责人、有效期、关联技能展示。
- [x] 删除本次造成的失效明细计算方法，不改filters或执行模块行为。

## Task 4：功能验证与交付

**Files:** docs/report-api.md；新增 scripts/report-sql-validation/verify.py、results.json、docs/report-sql-validation.md。

- [x] 改造前将当前源码保留为本机对照副本，启动对照和改造两个临时服务，共用现有验证库，不写入数据库。
- [x] 读取现有manifest生成10个接口及summary/filters回归请求，比较完整HTTP和JSON；保留至少一项非零结果断言，避免空响应伪通过。
- [x] 覆盖全行/分行/支行、等级、单多技能、首尾日、空范围、无执行、NULL链路、跨批次、下钻越界、来源标签、排序、TOPN与每日补零。
- [x] 对Review Focus五项分别记录实际覆盖场景；现有数据无法覆盖的并列/大小写等边界明确列出，不宣称已测试。
- [x] 运行修改Java文件的java-quality.sh verify及全量backend-gate.sh src/main/java/；检查XML、验证脚本语法及文档链接。
- [x] 同步接口说明的数据处理方式，记录实际检查命令、结果和限制；停止本轮临时服务。

## 自检结论

任务边界覆盖其余10个接口，依赖集中于公共数据库聚合契约，宜在当前会话顺序实施。无永久建表、缓存、监控或新接口。Java保留小规模维度响应和展示关系，不承诺零内存；数据库内部物化及性能仍需后续单独评估。

## 执行记录

已完成，423项只读功能对照通过；9个Java文件verify及92个Java文件全量门禁通过。修正规划明细的计划假设：其当前代码保留无执行任务，不能同覆盖表一起排除；已在实现中分开处理。详情见../../report-sql-validation.md。
