package com.cmbchina.application.report.api.dto;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

/** 支行客户经理汇总。 */
@Getter
@Setter
@Builder
public class ExecutionManagerSummary {
    private long managerCount;
    private long taskCount;
    private long recommendedCustomerCount;
    private BigDecimal contactRate;

}
