package com.cmbchina.application.report.api.dto;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
/** 单日执行走势。 */
@Getter
@Setter
@Builder
public class ExecutionTrendPoint {
    private LocalDate date;
    @JsonUnwrapped
    private ExecutionMetricsResponse metrics;
}
