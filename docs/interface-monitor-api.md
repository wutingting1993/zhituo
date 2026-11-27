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

日期按服务本地日期校验，最早为当天向前推六个自然月。服务通过 MyBatis 和 ClickHouse JDBC 查询 `LQ13_DataGovernance.interface_monitor_offline_daily_all`，按 `stat_date` 过滤分区，通过表名后的 `FINAL` 对底层替换表去重（兼容 ClickHouse 22.3），再聚合统计和覆盖记录。自定义日期的截止边界为结束日期次日零点。接口、专家名称按聚合键批量查询同一个 MySQL 库的 `api_interface`、`operation_task`。

`data` 包含以下字段。

- `items` 的行包含 `key`、`name`、`bucketStart`、`bucketEnd`、`effectiveSeconds`、`execution` 和 `upstream`。桶时间为本地日期时间字符串，格式为 `yyyy-MM-dd HH:mm:ss.SSS`，例如 `2026-11-19 10:00:00.000`。
- 两种口径均返回 `started_count`、`success_count`、`failure_count`、`timeout_count`、`cancelled_count`、`cache_read_count`、`cache_hit_count`、`expert_count`、`interface_count`、`average_qps`、`success_tps`、`error_rate`、`timeout_rate`、`cache_hit_rate`、`p95_millis`、`p99_millis`。错误率使用配置接口的业务成功判断，指标无统计口径时比率和耗时返回 null。
- `coverage` 的行包含 `available_from`、`available_through`、`effective_seconds`、`statistics_completed_at`。时间字段可为 null，非空值使用同一毫秒格式。
- `statisticsCompletedAt` 是所查询的离线数据中最晚的统计完成时间，使用同一毫秒格式，无覆盖时返回 null。

### 吞吐指标

`overview`、`interfaces`、`experts`、`summary`、`trend` 中返回的完整 `execution`、`upstream` 指标对象均包含以下字段。各 POST 请求的参数、校验和错误码沿用现有约定。

| 字段 | 类型 | 必填 / 可空 | 说明 |
| --- | --- | --- | --- |
| active_second_p95_qps | integer (int64) | 必填、可空 | 活跃秒开始次数的第 95 百分位 |
| active_second_p95_success_tps | integer (int64) | 必填、可空 | 活跃秒成功完成次数的第 95 百分位 |
| peak_qps | integer (int64) | 必填、可空 | 活跃秒开始次数的最大值 |

自然秒按事件发生时间划分。开始或结束事件使该秒成为活跃秒。两个 P95 使用同一组活跃秒，缺少开始或成功完成事件的秒分别计 0。先在查询分组和指标口径内按同一秒合并计数，再排序取第 `ceil(0.95 × 活跃秒数)` 项。没有活跃秒或分组包含尚未生成秒级计数的指标行时，三项均为 null。

请求示例 `POST /api/v1/interface-monitor/interfaces`，请求体为 `{"startDate":"2026-11-19","endDate":"2026-11-19"}`。响应指标片段如下。

```json
{"active_second_p95_qps":20,"active_second_p95_success_tps":18,"peak_qps":31}
```

## POST /api/v1/interface-monitor/refresh

`periodType` 为必填字符串，取值 `day`、`week`、`month`，分别从当天、本周周一、本月第一天开始。`offset` 为整数，默认 0，取值 0 至 4294967295。0 表示当前周期，只更新截至触发时已闭合的 10 分钟桶。正数向历史回退相应周期并更新整个周期。无闭合桶时直接成功。无效周期、偏移或早于 ClickHouse Date 起点的日期返回 `10000`。

应用通过 MyBatis 和 ClickHouse JDBC 从 `interface_monitor_event` 生成指标和覆盖数据，逐日通过 `INSERT INTO` 直接写入 `interface_monitor_offline_daily`。正式表使用 `ReplicatedReplacingMergeTree()`，以 `(stat_date, bucket_start, row_kind, interface_id, agent_id, metric_scope)` 为覆盖键，重算时整行替换计数、耗时聚合状态和桶内活跃秒计数。上线前先在各分片及查询分布式表应用 [秒级计数列迁移](../database/clickhouse-monitor-second-counts.sql)。仅对原始事件仍完整保留的历史周期调用刷新回填。每个日期先写指标，再写覆盖记录与统计完成时间。数据库操作失败返回 `10006`，已写入的数据保留，可重试同一周期。请求示例为 `{"periodType":"week","offset":1}`，成功响应为 `{"code":200,"message":"成功","data":null,"success":true}`。

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

### 总览请求结果分布

POST `/api/v1/interface-monitor/overview` 的请求参数及原有响应字段保持现有约定，新增以下字段，均由后端按当前日期、接口和专家条件计算。

| 字段 | 类型 | 必填 / 可空 | 说明 |
| --- | --- | --- | --- |
| data.overview.completedCount | integer (int64) | 必填、非空 | 已结束调用数，成功 + 失败 + 取消 |
| data.overview.resultDistribution | array | 必填、非空 | 固定按成功、其他失败、超时、取消排序的四项 |
| resultDistribution[].name | string | 必填、非空 | 分类名称 |
| resultDistribution[].count | integer (int64) | 必填、非空 | 分类次数 |
| resultDistribution[].percentage | number | 必填、可空 | count / completedCount × 100，单位为百分数，四舍五入保留两位小数，分母为零时为 null |

超时属于失败，其他失败 = 失败 - 超时，四项互斥。分母包含取消，不包含尚未结束的调用，与错误率的分母不同。独立四舍五入后，占比总和可能与 100 存在舍入差异。无数据时 completedCount 和四项 count 均为 0，四项 percentage 均为 null。参数校验与错误码沿用本接口约定。

请求示例 `{"startDate":"2026-11-25","endDate":"2026-11-25"}`。响应新增字段片段如下，位于成功信封的 data.overview 中。

```json
{
  "completedCount": 144,
  "resultDistribution": [
    {"name": "成功", "count": 123, "percentage": 85.42},
    {"name": "其他失败", "count": 7, "percentage": 4.86},
    {"name": "超时", "count": 6, "percentage": 4.17},
    {"name": "取消", "count": 8, "percentage": 5.56}
  ]
}
```

## POST /api/v1/interface-monitor/batch-metrics

按专家和日期统计两个批量入口的完整请求周期，接受现有 JSON 请求字段。startDate、endDate 必填，格式和六个月日期校验沿用现有规则。agentId 为可选字符串，unattributedOnly 为可选布尔值、默认 false，两者互斥。interfaceId 对本接口无筛选作用，内部接口表格由原查询处理。请求示例为 `{"startDate":"2026-11-27","endDate":"2026-11-27","agentId":"local-expert-1"}`。

成功响应沿用 code=200 信封，data 字段如下。参数错误返回 10000，ClickHouse 查询失败返回 10006。

| 字段 | 类型 | 必填 / 可空 | 含义 |
| --- | --- | --- | --- |
| summary | object | 必填、可空 | 已覆盖分钟合并后的整体指标，无覆盖为 null |
| points | array | 必填、非空 | 每个已覆盖自然分钟的指标，按时间升序，空闲分钟为 0 |
| coverage | array | 必填、非空 | 使用已有 available_from、available_through、effective_seconds、statistics_completed_at 结构，逐分钟发布 |
| statisticsCompletedAt | string | 必填、可空 | 最近统计发布时间，毫秒本地时间 |
| bucketSeconds | integer | 必填、非空 | 固定 60 |

summary 和 points 的每项均返回下表字段，计数为非负 int64。bucketStart、bucketEnd 为 `yyyy-MM-dd HH:mm:ss.SSS` 本地时间，结束边界不包含。summary 的边界覆盖全部已发布分钟，可能存在缺口，速率分母仅累计已覆盖分钟秒数。

| 指标 | 类型 | 必填 / 可空 | 口径 |
| --- | --- | --- | --- |
| startedCount | integer | 必填、非空 | 窗口内接收请求数 |
| successCount | integer | 必填、非空 | 所有查询项成功的完成请求数 |
| partialFailureCount | integer | 必填、非空 | 同时包含成功及失败项的完成请求数 |
| failureCount | integer | 必填、非空 | 全部失败或 HTTP 请求异常数 |
| cancelledCount | integer | 必填、非空 | 取消数，排除于完成 TPS 和耗时样本 |
| completedCount | integer | 必填、非空 | successCount + partialFailureCount + failureCount |
| latencySampleCount | integer | 必填、非空 | 已完成且记录耗时的样本数 |
| averageMillis | number | 必填、可空 | 总耗时除以样本数，无样本为 null |
| p95Millis | number | 必填、可空 | 合并分钟耗时分布后取 P95，无样本为 null |
| qps | number | 必填、非空 | startedCount / 覆盖秒数 |
| completedTps | number | 必填、非空 | completedCount / 覆盖秒数 |
| successTps | number | 必填、非空 | successCount / 覆盖秒数 |
| partialFailureRate | number | 必填、可空 | partialFailureCount / completedCount，无完成请求为 null |
| failureRate | number | 必填、可空 | failureCount / completedCount，无完成请求为 null |
| bucketStart、bucketEnd | string | 必填、非空 | 指标对应范围的起止时间 |

开始次数按接收时间归属，完成次数和耗时按响应写出终态时间归属。跨窗口完成时，开始与完成可归属不同分钟。耗时包括服务端接收、业务执行、结果转换及响应写出，无法测量客户端收完响应的时刻。成功、部分失败及失败参与耗时，取消单列。

无数据响应示例为 `{"code":200,"message":"成功","success":true,"data":{"summary":null,"points":[],"coverage":[],"statisticsCompletedAt":null,"bucketSeconds":60}}`。一个空闲分钟的指标示例为 `{"startedCount":0,"successCount":0,"partialFailureCount":0,"failureCount":0,"cancelledCount":0,"completedCount":0,"latencySampleCount":0,"averageMillis":null,"p95Millis":null,"qps":0,"completedTps":0,"successTps":0,"partialFailureRate":null,"failureRate":null,"bucketStart":"2026-11-27 00:00:00.000","bucketEnd":"2026-11-27 00:01:00.000"}`。

### 迁移和数据覆盖

本机表结构见 [批量监控迁移](../database/clickhouse-monitor-batch.sql)，其集群名为 local_monitor。部署到其他集群时应采用实际集群名及复制表引擎，并保证同一事件或离线覆盖键写入同一分片。查询使用分布式表后的 FINAL 去重，事件重发及同键重算不会重复累加。

批量分钟统计随现有 refresh 请求生成，只发布已闭合的自然分钟。配置 `BATCH_MONITOR_AVAILABLE_FROM` 为完整采集链路生效后的第一个完整自然分钟，格式为 `yyyy-MM-dd HH:mm:ss.SSS`。默认空字符串时不发布批量覆盖，避免把历史缺少终态的时间误报为空闲。回填前须确认开始和结束事件在该范围完整保留。重算按已有 ReplacingMergeTree 整行替换模式，刷新中可能读到新旧指标，统计失败需重试同一周期。

迁移文件不会启动 Kafka 或发布 Flink 作业。引擎事件映射需同步采用 api-config 项目 sql/flink/interface_monitor_kafka_to_clickhouse.sql 的 BATCH_STARTED/BATCH_FINISHED 映射。若事件被迟到写入已发布分钟，应重算受影响的日期。

本机验收依次执行 [批量样例](../database/clickhouse-monitor-batch-local-seed.sql)和[内部接口样例](../database/clickhouse-monitor-batch-local-interface-seed.sql)，再调用 refresh 生成统计。样例使用 local-batch- 前缀、local-expert-1、local-expert-2 及未归属专家，和正式业务请求区分。重复执行样例通过事件标识去重。
