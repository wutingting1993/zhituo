package com.cmbchina.application.monitor.api.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

/** 页面展示的概览及三个排行榜。 */
@Getter
@Builder
public class MonitorOverviewResponse {
    private final long executionCount;
    private final long completedCount;
    private final List<MonitorDistributionResponse> resultDistribution;
    private final long failureCount;
    private final long affectedInterfaceCount;
    private final long affectedExpertCount;
    private final Double errorRate;
    private final Double p95Millis;
    private final List<MonitorItemResponse> interfaceFailureTop;
    private final List<MonitorItemResponse> interfaceLatencyTop;
    private final List<MonitorItemResponse> expertFailureTop;
}
