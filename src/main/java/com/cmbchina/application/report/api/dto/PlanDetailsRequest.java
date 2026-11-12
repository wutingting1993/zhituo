package com.cmbchina.application.report.api.dto;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Setter;
/** 规划明细下钻条件，检索范围与点击机构独立传入。 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanDetailsRequest {
    @Valid
    @NotNull(message = "检索条件不能为空")
    private ReportSummaryRequest filters;
    private String scopeBbkId;
    private String scopeOrgId;
    private String source;

    /** 空字符串与未传参数保持一致。 */
    public String getScopeOrgId() {
        return scopeOrgId != null && scopeOrgId.isEmpty() ? null : scopeOrgId;
    }

    /** 空字符串与未传参数保持一致。 */
    public String getSource() {
        return source != null && source.isEmpty() ? null : source;
    }
}
