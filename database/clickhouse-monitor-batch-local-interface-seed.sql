-- 仅用于本机验收。将样例批次展开成配置接口阶段，便于专家抽屉对照。
INSERT INTO LQ13_DataGovernance.interface_monitor_event
    (event_id, occurred_at, interface_id, agent_id, metric_scope, event_phase,
     outcome, failure_kind, cache_result, elapsed_millis)
SELECT
    concat(event_id, '-execution-', toString(item)),
    occurred_at,
    concat('local-iface-', toString(item % 3 + 1)),
    agent_id,
    'EXECUTION',
    event_phase,
    multiIf(event_phase = 'START', '', outcome = 'PARTIAL_FAILURE',
        if(item = 0, 'SUCCESS', 'FAILURE'), outcome),
    if(event_phase = 'FINISH' AND (outcome = 'FAILURE' OR (outcome = 'PARTIAL_FAILURE' AND item > 0)), 'BUSINESS', ''),
    if(event_phase = 'FINISH', 'MISS', ''),
    elapsed_millis
FROM LQ13_DataGovernance.interface_monitor_batch_event FINAL
ARRAY JOIN range(2) AS item
WHERE startsWith(event_id, 'local-batch-');
