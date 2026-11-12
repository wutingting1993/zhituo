package com.cmbchina.application.report.api.dto;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/** 支行执行下钻条件。 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionDetailsRequest {
    @Valid
    @NotNull(message = "检索条件不能为空")
    private ReportSummaryRequest filters;
    @NotBlank(message = "请选择下钻分行")
    private String scopeBbkId;
    @NotBlank(message = "请选择下钻支行")
    private String scopeOrgId;

}
