package com.cmbchina.application.monitor.api.dto;

import lombok.Builder;
import lombok.Getter;

/** 已结束调用的互斥分类及百分比。 */
@Getter
@Builder
public class MonitorDistributionResponse {
    private final String name;
    private final long count;
    private final Double percentage;
}
