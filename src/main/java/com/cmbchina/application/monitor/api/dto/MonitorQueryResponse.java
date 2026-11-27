package com.cmbchina.application.monitor.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/** 页面查询的指标及离线覆盖信息。 */
@Getter
@Builder
@Setter
public class MonitorQueryResponse {
    private final List<MonitorItemResponse> items;
    private final List<MonitorCoverageResponse> coverage;
    private final String statisticsCompletedAt;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private MonitorOverviewResponse overview;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean cacheEnabled;
}
