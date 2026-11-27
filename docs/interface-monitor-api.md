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

## POST /api/v1/interface-monitor/experts

专家列表沿用现有成功信封和 `data.items` 结构，新增 `items[].batch`，使用与 `/batch-metrics` 的 `summary` 相同的字段、类型与完整请求周期口径。内部 `execution` / `upstream` 字段保留用于已有内部调用数据，不作为专家列表的展示指标。专家候选取内部执行与批量请求中出现过的专家并集，包含未归属。

请求参数 `startDate`、`endDate` 为必填日期字符串，`agentId` 为可选字符串，`unattributedOnly` 为可选布尔值、默认 false，与非空 `agentId` 互斥。`interfaceId` 即使传入也不参与此接口筛选。日期校验、HTTP 200 信封及错误码 10000 / 10006 沿用上文。

`batch` 为可选对象：查询范围没有批量统计覆盖时省略，前端显示「—」；已有覆盖但该专家无批量事件时返回零计数、零吞吐，耗时和无分母比率为 null。其 `bucketStart` / `bucketEnd` 在零事件对象中可为 null，列表不使用这两个字段。专家聚合先筛选已发布覆盖的分钟，再按专家合并两个入口的计数与耗时分布。每个专家的 QPS 和成功 TPS 分母均为查询范围内全部已覆盖分钟的秒数，包含空闲秒，跨天 P95 合并分布后计算。`coverage` 和 `statisticsCompletedAt` 对应批量请求覆盖，无批量覆盖时分别为 `[]` 和 null。

页面展示批量请求数、成功率、平均耗时、P95 耗时、QPS 和成功 TPS；成功率为 `successCount / completedCount`，已完成包含全部成功、部分失败和失败，取消不参与分母，无已完成批次时为 null。列表按批量请求数降序，吞吐单位为次/秒。

请求示例：`{"startDate":"2026-11-27","endDate":"2026-11-27","agentId":"local-expert-1"}`。成功响应的批量指标片段（位于 `data.items[0]`，省略保留的内部指标字段）：

```json
{
  "key": "local-expert-1",
  "name": "财富顾问",
  "batch": {
    "startedCount": 180,
    "successCount": 120,
    "partialFailureCount": 40,
    "failureCount": 20,
    "cancelledCount": 0,
    "completedCount": 180,
    "latencySampleCount": 180,
    "averageMillis": 5414.444444444444,
    "p95Millis": 70000,
    "qps": 0.05,
    "completedTps": 0.05,
    "successTps": 0.03333333333333333,
    "partialFailureRate": 0.2222222222222222,
    "failureRate": 0.1111111111111111,
    "bucketStart": "2026-11-27 00:00:00.000",
    "bucketEnd": "2026-11-27 01:00:00.000"
  }
}
```

示例假设覆盖 3600 秒，实际吞吐按返回的已覆盖时长计算。

## POST /api/v1/interface-monitor/summary：接口缓存状态

请求沿用现有日期、接口和专家筛选：`startDate`、`endDate` 为必填 `YYYY-MM-DD` 字符串，`interfaceId`、`agentId` 为可选字符串，`unattributedOnly` 为可选布尔值、默认 false，与非空 `agentId` 互斥。校验、成功信封及错误码 10000 / 10006 沿用上文。

响应新增 `data.cacheEnabled`：可选 boolean，关联 `api_interface.id = interfaceId` 与 `api_interface_version.interface_id`，按 `published_version = version_no` 读取当前已发布配置的 `cache_enabled`。false 表示明确关闭，true 表示明确开启。未指定接口、接口已删除、无已发布版本或开关为空时省略该字段，表示无法确定；不会使用草稿开关或命中率推断。该状态是当前发布配置，不代表查询历史日期的配置。

页面仅在 `cacheEnabled: false` 时在卡片内容区居中显示「未开启缓存」，隐藏百分比和缓存命中率区间图；true 时即使命中率为 0 也保留区间图；字段缺失时显示「缓存配置未知」并保留原图，不标记未开启。累计指标与其他图表保持原口径。

请求示例：`{"startDate":"2026-11-27","endDate":"2026-11-27","interfaceId":"local-iface-1"}`。响应新增字段片段：`{"cacheEnabled":false}`，位于原成功信封的 `data` 内；原有 items、coverage、statisticsCompletedAt 保留。配置未关联时原响应中不出现 cacheEnabled。

本机缓存四态验收：执行 [MySQL 缓存配置样例](../database/mysql-monitor-cache-local-seed.sql)，仅调整 local-iface-1/2/3/4。账户查询（local-iface-1）关闭缓存，资产与理财持仓汇总（local-iface-2）开启缓存且当前样例命中率为 0，长名称批量查询接口（local-iface-3）不关联发布版本、配置未知。前三种沿用现有 ClickHouse 监控样例。再执行 [ClickHouse 命中样例](../database/clickhouse-monitor-cache-hit-local-seed.sql) 并刷新当天统计，新增「缓存命中验证查询」（local-iface-4）：缓存开启，20 次成功执行，10 次命中与 10 次未命中，命中率 50%。事件固定标识，重复执行同一天样例按 FINAL 去重。

## POST /api/v1/interface-monitor/expert-executions

用于接口展开下的领域专家对比，统计对象为**当前接口内**按专家分组的接口逻辑调用和上游实际请求，不统计整批 HTTP 请求。复用内部通用聚合查询；原 `/experts` 的批量请求列表仍忽略接口筛选，不受本次调整影响。

| 请求字段 | 类型 | 必填/可空 | 规则 |
| --- | --- | --- | --- |
| startDate | string | 必填、不可空 | YYYY-MM-DD，沿用最近六个月日期校验 |
| endDate | string | 必填、不可空 | YYYY-MM-DD，包含结束日，不能早于开始日 |
| interfaceId | string | 必填、不可空 | 非空白接口标识，所有返回指标均限定该接口 |
| agentId | string | 可选、可空 | 进一步限定专家 |
| unattributedOnly | boolean | 可选，默认 false | 与非空 agentId 互斥 |

成功响应沿用 `code:200`、`success:true` 信封。`data.items` 为专家分组数组，每项 key/name、bucketStart/bucketEnd、effectiveSeconds、execution/upstream 的字段、类型与空值口径沿用本文通用监控指标定义；不返回 batch。key 可为空字符串表示未归属，execution/upstream 始终为对象。data.coverage 和 data.statisticsCompletedAt 使用内部执行统计覆盖。无匹配事件时 items 为 []，无覆盖时 statisticsCompletedAt 可为 null。无配置专家名称时沿用名称回退规则。

占比由前端分别按该接口内 execution.started_count、upstream.started_count 总数计算，不能混用分母；分母为 0 显示「—」。专家上下两行共用六指标列，按所选口径的原始指标值排序，缺失值置底，分组两行不拆开。

接口、日期或专家条件无效返回业务错误码 10000（缺少接口提示「请选择需要对比的接口」）；统计数据源错误沿用 10006，HTTP 200 业务信封约定保持一致。

请求示例：`{"startDate":"2026-11-27","endDate":"2026-11-27","interfaceId":"local-iface-4"}`。实际样例响应 data.items[0] 的指标片段：`{"key":"local-expert-1","name":"财富顾问","execution":{"started_count":20,"cache_read_count":20,"cache_hit_count":10,"cache_hit_rate":0.5},"upstream":{"started_count":0}}`。完整响应同时保留上述通用指标对象的所有字段、覆盖和统计完成时间；本例省略未变化字段。
