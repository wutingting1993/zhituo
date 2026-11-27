-- 本机 ClickHouse 22.3。部署到其他集群时使用实际集群名及复制表引擎。
CREATE TABLE IF NOT EXISTS LQ13_DataGovernance.interface_monitor_batch_event
(
    event_id String COMMENT '阶段事件标识，传输重发沿用',
    schema_version UInt16 COMMENT '事件契约版本',
    batch_id String COMMENT '批量请求标识',
    occurred_at DateTime64(3, 'Asia/Shanghai') COMMENT '阶段发生时间',
    occurred_date Date MATERIALIZED toDate(occurred_at) COMMENT '事件日期，北京时间',
    entrypoint String COMMENT '批量查询入口路径',
    agent_id String COMMENT '专家标识，空字符串表示未归属',
    event_phase String COMMENT 'START 或 FINISH',
    outcome String COMMENT 'SUCCESS、PARTIAL_FAILURE、FAILURE 或 CANCELLED',
    elapsed_millis Nullable(UInt64) COMMENT '服务端响应写出完成时的整批耗时毫秒'
)
ENGINE = ReplacingMergeTree
PARTITION BY occurred_date
ORDER BY (occurred_date, event_id)
COMMENT '批量入口完整请求周期的阶段事件';

CREATE TABLE IF NOT EXISTS LQ13_DataGovernance.interface_monitor_batch_event_all
AS LQ13_DataGovernance.interface_monitor_batch_event
ENGINE = Distributed('local_monitor', 'LQ13_DataGovernance', 'interface_monitor_batch_event', rand())
COMMENT '批量入口完整请求周期的阶段事件分布式查询表';

CREATE TABLE IF NOT EXISTS LQ13_DataGovernance.interface_monitor_batch_minute
(
    stat_date Date COMMENT '统计日期，北京时间',
    bucket_start DateTime64(3, 'Asia/Shanghai') COMMENT '自然分钟起点',
    row_kind String COMMENT 'METRIC 指标行或 COVERAGE 已发布覆盖',
    statistics_completed_at Nullable(DateTime64(3, 'Asia/Shanghai')) COMMENT '分钟统计发布时间',
    entrypoint String COMMENT '批量入口，覆盖记录为空',
    agent_id String COMMENT '专家标识，空字符串表示未归属',
    started_count UInt64 COMMENT '接收到的请求数',
    success_count UInt64 COMMENT '全部查询项成功的完成请求数',
    partial_failure_count UInt64 COMMENT '部分查询项失败的完成请求数',
    failure_count UInt64 COMMENT '全部查询项失败或请求异常数',
    cancelled_count UInt64 COMMENT '取消请求数，不计入完成 TPS',
    latency_sum_millis UInt64 COMMENT '成功、部分失败和失败请求的总耗时',
    latency_sample_count UInt64 COMMENT '耗时样本数，不包含取消',
    latency_state AggregateFunction(quantilesTDigestIf(0.95, 0.99), UInt64, UInt8) COMMENT '可合并的整批耗时分布'
)
ENGINE = ReplacingMergeTree
PARTITION BY stat_date
ORDER BY (stat_date, bucket_start, row_kind, entrypoint, agent_id)
COMMENT '专家批量请求分钟离线指标和覆盖';

CREATE TABLE IF NOT EXISTS LQ13_DataGovernance.interface_monitor_batch_minute_all
AS LQ13_DataGovernance.interface_monitor_batch_minute
ENGINE = Distributed('local_monitor', 'LQ13_DataGovernance', 'interface_monitor_batch_minute', rand())
COMMENT '专家批量请求分钟离线指标和覆盖分布式查询表';
