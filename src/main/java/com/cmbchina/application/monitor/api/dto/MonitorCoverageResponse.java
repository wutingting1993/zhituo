package com.cmbchina.application.monitor.api.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Getter;

/** 页面展示的已统计区间。 */
@Getter
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class MonitorCoverageResponse {
    private final String availableFrom;
    private final String availableThrough;
    private final double effectiveSeconds;
    private final String statisticsCompletedAt;
}
