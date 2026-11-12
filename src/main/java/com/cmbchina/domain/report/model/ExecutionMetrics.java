package com.cmbchina.domain.report.model;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/** 执行视角各阶段统计。 */
@Getter
@Setter
@Builder
public class ExecutionMetrics {
    private ReportStatistics statistics;
    private long insightCustomerCount;
    private long callCustomerCount;

}
