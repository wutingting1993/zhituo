package com.cmbchina.application.report.api.dto;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

/** 执行阶段数量与百分比。 */
@Getter
@Setter
@Builder
public class ExecutionMetricsResponse {
    private long taskCount;
    private long scheduledTaskCount;
    private long executedTaskCount;
    private long pendingTaskCount;
    private long failedTaskCount;
    private long recommendedCustomerCount;
    private long viewCustomerCount;
    private long insightCustomerCount;
    private long callCustomerCount;
    private long contactCustomerCount;
    private BigDecimal viewRate;
    private BigDecimal insightRate;
    private BigDecimal callRate;
    private BigDecimal contactRate;

}
