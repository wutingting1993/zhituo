package com.cmbchina.application.report.api.dto;
import javax.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
/** 规划来源排名检索条件。 */
@Getter
@Setter
@Builder(builderMethodName = "sourcesBuilder")
@NoArgsConstructor
@AllArgsConstructor
public class PlanSourcesRequest extends ReportSummaryRequest {
    private static final int DEFAULT_TOP_N = 5;
    @Min(value = 1, message = "topN必须为正整数")
    private Integer topN;
    /** 未指定时保持默认返回五项。 */
    public Integer getTopN() {
        return topN == null ? DEFAULT_TOP_N : topN;
    }
}
