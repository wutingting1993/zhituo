package com.cmbchina.application.report.api.dto;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

/** 按最小流失率选出的环节。 */
@Getter
@Setter
@Builder
public class ExecutionLossResponse {
    private String fromStage;
    private String toStage;
    private long customerDifference;
    private BigDecimal lossRate;

}
