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

日期按北京时间，最早为当天向前推六个自然月。查询读取离线已发布统计，自定义日期的截止边界为结束日期次日零点。接口、专家名称按聚合键批量查询同一个 MySQL 库的 `api_interface`、`operation_task`。

`data` 包含以下字段。

- `items` 的行包含 `key`、`name`、`bucketStart`、`bucketEnd`、`effectiveSeconds`、`execution` 和 `upstream`。桶时间为北京时间 ISO 8601，精确到毫秒。
- 两种口径均返回 `started_count`、`success_count`、`failure_count`、`timeout_count`、`cancelled_count`、`cache_read_count`、`cache_hit_count`、`expert_count`、`interface_count`、`average_qps`、`success_tps`、`error_rate`、`timeout_rate`、`cache_hit_rate`、`p95_millis`、`p99_millis`。错误率使用配置接口的业务成功判断，指标无统计口径时比率和耗时返回 null。
- `coverage` 的行包含 `available_from`、`available_through`、`effective_seconds`、`statistics_completed_at`。
- `statisticsCompletedAt` 是所查询的离线数据中最晚的统计完成时间，无覆盖时返回 null。

## POST /api/v1/interface-monitor/refresh

`periodType` 必填，取值 `day`、`week`、`month`。`offset` 默认 0，取值 0 至 4294967295。该接口直接转发外部 CK API `interface-monitor-offline-refresh`，原样返回其 `data`。用例用于页面的「更新统计」，离线任务平台仍按现有方式调用统计接口。

配置 `APP_MONITOR_CLICKHOUSE_BASE_URL` 启用实际 CK API。Mock 联调时给智拓进程设置 `APP_MONITOR_MOCK_ENABLED=true`，两种模式使用相同的页面契约。

### 全局汇总与排行榜

现有 POST `/interface-monitor/query` 在 `groupBy: "total"`、`bucketSeconds: 0` 且无接口或专家条件时，在成功响应的 `data` 中追加 `overview`。日期校验和错误码沿用上文。

`executionCount`、`failureCount`、`affectedInterfaceCount`、`affectedExpertCount` 为非负整数。受影响数仅计有失败的分组，专家数排除未归属。 `errorRate` 为失败数除以成功加失败数，`p95Millis` 为整体执行耗时百分位。无分母或样本时对应值为 null。

`interfaceFailureTop`、`interfaceLatencyTop`、`expertFailureTop` 为数组，每榜最多 10 项，按指标降序，再按名称和标识排序。每项包含字符串 `key`、`name` 及执行指标 `execution`。失败榜展示 `failure_count` 和可空的 `error_rate`，耗时榜展示 `p95_millis` 和非负整数 `latency_sample_count`。

请求示例：`{"startDate":"2026-11-19","endDate":"2026-11-19","groupBy":"total","bucketSeconds":0}`。

空数据的 `overview` 示例：
```json
{"executionCount":0,"failureCount":0,"affectedInterfaceCount":0,"affectedExpertCount":0,"errorRate":null,"p95Millis":null,"interfaceFailureTop":[],"interfaceLatencyTop":[],"expertFailureTop":[]}
```
