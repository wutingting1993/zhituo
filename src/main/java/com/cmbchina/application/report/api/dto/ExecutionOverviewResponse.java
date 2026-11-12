package com.cmbchina.application.report.api.dto;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

/** 执行链路概览。 */
@Getter
@Setter
@Builder
public class ExecutionOverviewResponse {
    @JsonUnwrapped
    private ExecutionMetricsResponse metrics;
    private long managerCount;
    private BigDecimal customersPerTask;
    private Long averageTaskCount;
    private Long averageRecommendedCustomerCount;
    private Long averageContactCustomerCount;
    private ExecutionLossResponse largestLoss;

}
