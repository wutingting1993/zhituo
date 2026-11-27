-- 先在各分片的本地表执行，再更新查询使用的分布式表。
ALTER TABLE LQ13_DataGovernance.interface_monitor_offline_daily
    ADD COLUMN IF NOT EXISTS second_counts Tuple(Array(UInt16), Array(UInt64), Array(UInt64), Array(UInt64))
    COMMENT '活跃秒在桶内的偏移、开始次数、成功完成次数和事件次数';

ALTER TABLE LQ13_DataGovernance.interface_monitor_offline_daily_all
    ADD COLUMN IF NOT EXISTS second_counts Tuple(Array(UInt16), Array(UInt64), Array(UInt64), Array(UInt64))
    COMMENT '活跃秒在桶内的偏移、开始次数、成功完成次数和事件次数';
