package com.cmbchina.application.report.api.dto;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

/** 机构整体基准率。 */
@Getter
@Setter
@Builder
public class ExecutionBenchmarkResponse {
    private BigDecimal viewRate;
    private BigDecimal insightRate;
    private BigDecimal callRate;
    private BigDecimal contactRate;

}
