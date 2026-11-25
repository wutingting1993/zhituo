package com.cmbchina.application.monitor.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/** 页面展示的分组及两种执行口径。 */
@Getter
@Builder
@Setter
public class MonitorItemResponse {
    private final String key;
    private String name;
    private final String bucketStart;
    private final String bucketEnd;
    private final double effectiveSeconds;
    private MonitorMetricsResponse execution;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private MonitorMetricsResponse upstream;
}
