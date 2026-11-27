-- 仅用于本机验收。事件标识固定，重复写入由 FINAL 去重。
-- 覆盖最近八天，每天 300 批，两个入口、两个专家及未归属。
INSERT INTO LQ13_DataGovernance.interface_monitor_batch_event
    (event_id, schema_version, batch_id, occurred_at, entrypoint, agent_id,
     event_phase, outcome, elapsed_millis)
SELECT
    concat('local-batch-', toString(toDate(started_at)), '-', toString(sample), '-', toString(phase)),
    toUInt16(1),
    concat('local-batch-', toString(toDate(started_at)), '-', toString(sample)),
    fromUnixTimestamp64Milli(toUnixTimestamp64Milli(started_at) + if(phase = 0, 0, toInt64(duration_ms)), 'Asia/Shanghai'),
    if(sample % 2 = 0, '/batch-query', '/batch-query/details'),
    multiIf(sample % 10 = 9, '', sample % 3 = 0, 'local-expert-2', 'local-expert-1'),
    if(phase = 0, 'START', 'FINISH'),
    if(phase = 0, '', multiIf(sample % 10 < 6, 'SUCCESS', sample % 10 < 8, 'PARTIAL_FAILURE',
        sample % 10 = 8, 'FAILURE', 'CANCELLED')),
    if(phase = 0, NULL, toUInt64(duration_ms))
FROM
(
    SELECT
        number % 300 AS sample,
        fromUnixTimestamp64Milli(toUnixTimestamp64Milli(
            toDateTime64(addDays(toStartOfDay(now('Asia/Shanghai')), -toInt32(intDiv(number, 300))), 3, 'Asia/Shanghai')
                + toIntervalMinute(5 + intDiv(sample, 10) * 2)
                + toIntervalSecond(if(sample % 17 = 0, 59, sample % 5))) + toInt64(sample % 10 * 50), 'Asia/Shanghai') AS started_at,
        toUInt64(if(sample % 17 = 0, 70000, 50 + (sample % 43) * 75)) AS duration_ms
    FROM numbers(2400)
)
ARRAY JOIN [0, 1] AS phase
WHERE fromUnixTimestamp64Milli(toUnixTimestamp64Milli(started_at) + toInt64(duration_ms), 'Asia/Shanghai') < now64(3, 'Asia/Shanghai') - INTERVAL 1 MINUTE;

-- 一批请求在前一天开始，下一天完成，用于核对跨天归属。
INSERT INTO LQ13_DataGovernance.interface_monitor_batch_event
    (event_id, schema_version, batch_id, occurred_at, entrypoint, agent_id,
     event_phase, outcome, elapsed_millis)
SELECT
    concat('local-batch-boundary-', toString(toDate(now('Asia/Shanghai'))), '-', toString(phase)),
    toUInt16(1),
    concat('local-batch-boundary-', toString(toDate(now('Asia/Shanghai')))),
    fromUnixTimestamp64Milli(toUnixTimestamp64Milli(
        toDateTime64(addDays(toStartOfDay(now('Asia/Shanghai')), -1), 3, 'Asia/Shanghai'))
        - if(phase = 0, toInt64(500), toInt64(-2500)), 'Asia/Shanghai'),
    '/batch-query/details',
    'local-expert-1',
    if(phase = 0, 'START', 'FINISH'),
    if(phase = 0, '', 'SUCCESS'),
    if(phase = 0, NULL, toUInt64(3000))
FROM numbers(1)
ARRAY JOIN [0, 1] AS phase;
