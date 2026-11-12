package com.cmbchina.application.report.api.dto;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/** 支行执行列表筛选与排序。 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionBranchesRequest {
    @Valid
    @NotNull(message = "检索条件不能为空")
    private ReportSummaryRequest filters;
    private String sortKey;
    private String sortDir;

}
