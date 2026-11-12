package com.cmbchina.application.report.api.dto;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
/** 技能参与度筛选及排名参数。 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillParticipationRequest {
    @Valid
    @NotNull(message = "检索条件不能为空")
    private ReportSummaryRequest filters;
    @Min(value = 1, message = "topN必须为正整数")
    private Integer topN;
    private String sortKey;

    /** 防止JSON小数被静默截断为整数排名数量。 */
    public void setTopN(BigDecimal value) {
        if (value == null) {
            topN = null;
            return;
        }
        try {
            topN = value.intValueExact();
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("topN必须为有效整数", exception);
        }
    }
}
