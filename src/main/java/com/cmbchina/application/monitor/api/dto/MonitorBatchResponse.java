package com.cmbchina.application.monitor.api.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

/** 展示批量请求整体汇总、分钟趋势及离线覆盖。 */
@Getter
@Builder
public class MonitorBatchResponse {
    private final MonitorBatchMetricResponse summary;
    private final List<MonitorBatchMetricResponse> points;
    private final List<MonitorCoverageResponse> coverage;
    private final String statisticsCompletedAt;
    private final int bucketSeconds;
}
