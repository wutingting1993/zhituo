# 专家批量请求监控实施计划

目标是交付已确认的批次完整周期监控。设计见 ../specs/2026-11-27-expert-batch-throughput-design.md。

约束为沿用现有技术栈，ClickHouse 22.3.2.1，修改文件静态检查及项目编译，单元测试和 e2e 需另行授权。保留既有改动。

- [x] 引擎在两个 HTTP 入口的接收至响应写完阶段产生 BATCH_STARTED/BATCH_FINISHED，共用 batchId。结果为 SUCCESS/PARTIAL_FAILURE/FAILURE/CANCELLED。同步 Flink 映射和事件文档。
- [x] 独立 interface_monitor_batch_event 明细及 interface_monitor_batch_minute 分钟离线表，按覆盖记录区分零值和缺失数据。复用现有刷新周期。
- [x] POST /api/v1/interface-monitor/batch-metrics 接受既有日期和专家参数，忽略 interfaceId。返回 summary、points、coverage、statisticsCompletedAt、bucketSeconds=60。
- [x] 指标采用 camelCase，包括 startedCount、successCount、partialFailureCount、failureCount、cancelledCount、completedCount、latencySampleCount、averageMillis、p95Millis、qps、completedTps、successTps、partialFailureRate、failureRate。points 增加 bucketStart 和 bucketEnd。无覆盖时 summary=null，耗时及无分母比率为空。有覆盖的空闲分钟计数和吞吐为 0。
- [x] 专家抽屉增加批量请求汇总和分钟吞吐趋势，按专家及日期查询。保留内部接口执行明细表格，移除内部接口汇总卡片，全部指标明确所属统计对象。
- [x] 同步契约，执行引擎与报表 Java 静态检查、编译及架构检查，前端格式、Lint、类型、元信息及构建检查。核对 ClickHouse SQL 样例和已授权本机链路。


## 本机验证记录

- ClickHouse 22.3.2.1 已创建批次阶段明细、分钟离线本地表及分布式表。
- 已写入 2026-11-20 至 2026-11-27 的本机样例，重复造数与重算后计数一致，另含 11 月 25 日开始、26 日完成的跨天批次。
- 智拓后端已重启，启用 BATCH_MONITOR_AVAILABLE_FROM=2026-11-20 00:00:00.000。
- 当天 local-expert-1 为 180 次开始、120 次全部成功、40 次部分失败、20 次失败。独立取消样例归入未归属专家。
- 分钟零值、跨分钟归属、历史缺失覆盖、接口筛选独立性及专家选项已通过 SQL/API 数据检查。
- 报表 Java verify、JDK8 编译及架构检查通过。引擎 JDK8 package 和目标文件 Checkstyle 通过。前端格式、Lint、类型、元信息及构建通过，浏览器已核对 1440 和 1920 宽度。
- 引擎真实 HTTP 端到端烟测被自动审批拒绝，等待单独授权。Kafka/Flink 外部作业未部署，本机页面当前使用已明确标记的数据库样例。


## 授权后的后续验证

用户已明确允许本机 HTTP 端到端验证。隔离引擎使用真实 HTTP 处理边界与控制器，可控业务返回覆盖成功、部分失败、全部失败、异常、取消及响应写出延迟。
测试发现响应转换或序列化异常被全局异常处理恢复为 HTTP 200 时，批次可能误判成功。修正方式为在错误产生及统一异常处理边界标记批次失败，继续在外层 HTTP 完成时发布唯一终态，保留原有响应。
- [x] 重跑全部真实 HTTP 场景并记录事件断言。
- [x] 按用户补充要求统一批量请求区块与主页布局、配色和指标展示，逐项提供可发现的 Tooltip。


真实 HTTP 最终结果为 20/20 通过，两个入口各十种场景。捕获 20 个开始、20 个完成及 14 个原始接收事件，终态为 2 次全部成功、2 次部分失败、14 次失败和 2 次取消。每个批次均恰好一个开始和一个终态。覆盖实际编码异常被恢复成 HTTP 200 的失败分类。
人为延后响应写出 300ms 后，两个入口分别在业务完成 323ms、301ms 后产生终态，符合完整请求周期定义。
验证采用真实 WebFlux、控制器及 HTTP 处理器，业务服务与发布器使用隔离夹具，未覆盖 Kafka/Flink 传输。隔离服务已退出，记录位于 /tmp/engine-http-smoke.log 和 /tmp/engine-http-fixture-events.ndjson。

批量区块已采用主页指标数字和分隔线样式、失败珊瑚色、图表语义配色及紫色选项。11 项指标及分钟趋势均提供可见帮助图标，鼠标悬停与键盘聚焦均已验证。前端全套检查与构建通过，1440 和 1920 宽度截图无裁切。

用户后续取消查询项统计。已移除采集事件字段、离线计算及查询响应，页面按三组展示并合并失败次数与对应占比。已部署历史列保留，新的写入和查询不使用。

最终页面调整：批量请求性能概览采用结果优先布局与结果环形图；分钟趋势沿用主页配色和面积填充，默认展示最近活跃一小时，覆盖范围内连接有效耗时及比率样本，未覆盖区间保留断点。下方仅保留“执行明细”表格。
