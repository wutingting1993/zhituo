# 接口调用监控 API

浏览器请求使用同源 `/zhituo/api/v1/interface-monitor`，报表代理移除 `/zhituo` 后转发到后端。请求和响应均为 JSON。成功响应使用智拓信封 `{"code":200,"message":"成功","data":...,"success":true}`，参数错误码为 `10000`，监控数据源错误码为 `10006`。

## POST /api/v1/interface-monitor/query

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| startDate | string | 必填，开始日期，`YYYY-MM-DD` |
| endDate | string | 必填，结束日期，包含该日 |
| groupBy | string | 必填，`interface`、`expert` 或 `total` |
| interfaceId | string | 指定平台配置接口 |
| agentId | string | 指定领域专家 |
| unattributedOnly | boolean | 默认 false，查询未归属调用。与 `agentId` 互斥 |
| bucketSeconds | integer | 默认 0，返回整个区间。正数须为 600 的倍数 |

日期按服务本地日期校验，最早为当天向前推六个自然月。服务通过 MyBatis 和 ClickHouse JDBC 查询 `LQ13_DataGovernance.interface_monitor_offline_daily_all`，按 `stat_date` 过滤分区，通过 `SETTINGS final = 1` 对底层替换表去重，再聚合统计和覆盖记录。自定义日期的截止边界为结束日期次日零点。接口、专家名称按聚合键批量查询同一个 MySQL 库的 `api_interface`、`operation_task`。

`data` 包含以下字段。

- `items` 的行包含 `key`、`name`、`bucketStart`、`bucketEnd`、`effectiveSeconds`、`execution` 和 `upstream`。桶时间为本地日期时间字符串，格式为 `yyyy-MM-dd HH:mm:ss.SSS`，例如 `2026-11-19 10:00:00.000`。
- 两种口径均返回 `started_count`、`success_count`、`failure_count`、`timeout_count`、`cancelled_count`、`cache_read_count`、`cache_hit_count`、`expert_count`、`interface_count`、`average_qps`、`success_tps`、`error_rate`、`timeout_rate`、`cache_hit_rate`、`p95_millis`、`p99_millis`。错误率使用配置接口的业务成功判断，指标无统计口径时比率和耗时返回 null。
- `coverage` 的行包含 `available_from`、`available_through`、`effective_seconds`、`statistics_completed_at`。时间字段可为 null，非空值使用同一毫秒格式。
- `statisticsCompletedAt` 是所查询的离线数据中最晚的统计完成时间，使用同一毫秒格式，无覆盖时返回 null。

## POST /api/v1/interface-monitor/refresh

`periodType` 为必填字符串，取值 `day`、`week`、`month`，分别从当天、本周周一、本月第一天开始。`offset` 为整数，默认 0，取值 0 至 4294967295。0 表示当前周期，只更新截至触发时已闭合的 10 分钟桶。正数向历史回退相应周期并更新整个周期。无闭合桶时直接成功。无效周期、偏移或早于 ClickHouse Date 起点的日期返回 `10000`。

应用通过 MyBatis 和 ClickHouse JDBC 从 `interface_monitor_event` 生成指标和覆盖数据，逐日通过 `INSERT INTO` 直接写入 `interface_monitor_offline_daily`。正式表使用 `ReplicatedReplacingMergeTree()`，以 `(stat_date, bucket_start, row_kind, interface_id, agent_id, metric_scope)` 为覆盖键，重算时整行替换计数和耗时聚合状态。每个日期先写指标，再写覆盖记录与统计完成时间。数据库操作失败返回 `10006`，已写入的数据保留，可重试同一周期。请求示例为 `{"periodType":"week","offset":1}`，成功响应为 `{"code":200,"message":"成功","data":null,"success":true}`。

生产环境使用 `CLICKHOUSE_JDBC_URL`、`CLICKHOUSE_USER`、`CLICKHOUSE_PASSWORD` 配置读写账号。JDBC 需指向可写的 ClickHouse 节点，正式表须是查询表 `interface_monitor_offline_daily_all` 的数据来源。同一覆盖键的写入必须路由到相同分片。刷新在单个实例内串行执行，部署时须保证只有一个刷新执行方。当前表未配置版本列，按写入顺序替换。刷新过程中可能读到新旧混合数据，失败后须重试。本方案按原始事件持续追加的口径运行，历史事件删除或改变接口、专家归属需要额外处理旧分组。Mock 联调时设置 `APP_MONITOR_MOCK_ENABLED=true`。

### 全局汇总与排行榜

现有 POST `/interface-monitor/query` 在 `groupBy: "total"`、`bucketSeconds: 0` 且无接口或专家条件时，在成功响应的 `data` 中追加 `overview`。日期校验和错误码沿用上文。

`executionCount`、`failureCount`、`affectedInterfaceCount`、`affectedExpertCount` 为非负整数。受影响数仅计有失败的分组，专家数排除未归属。 `errorRate` 为失败数除以成功加失败数，`p95Millis` 为整体执行耗时百分位。无分母或样本时对应值为 null。

`interfaceFailureTop`、`interfaceLatencyTop`、`expertFailureTop` 为数组，每榜最多 10 项，按指标降序，再按名称和标识排序。每项包含字符串 `key`、`name` 及执行指标 `execution`。失败榜展示 `failure_count` 和可空的 `error_rate`，耗时榜展示 `p95_millis` 和非负整数 `latency_sample_count`。

请求示例：`{"startDate":"2026-11-19","endDate":"2026-11-19","groupBy":"total","bucketSeconds":0}`。

空数据的 `overview` 示例：
```json
{"executionCount":0,"failureCount":0,"affectedInterfaceCount":0,"affectedExpertCount":0,"errorRate":null,"p95Millis":null,"interfaceFailureTop":[],"interfaceLatencyTop":[],"expertFailureTop":[]}
```
