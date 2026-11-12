package com.cmbchina.application.report.api.dto;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

/** 支行执行指标及机构比较。 */
@Getter
@Setter
@Builder
public class ExecutionBranchRow {
    private String bbkId;
    private String bbkName;
    private String orgId;
    private String orgName;
    private long skillCount;
    @JsonUnwrapped
    private ExecutionMetricsResponse metrics;
    private BigDecimal viewRateDifference;
    private BigDecimal insightRateDifference;
    private BigDecimal callRateDifference;
    private BigDecimal contactRateDifference;
    private String viewRateStatus;
    private String insightRateStatus;
    private String callRateStatus;
    private String contactRateStatus;

}
