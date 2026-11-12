package com.cmbchina.application.report.api.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.util.List;
import java.util.Collections;
import com.google.common.collect.Lists;
import java.time.LocalDate;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import javax.validation.constraints.NotNull;
/** 报表ReportSummaryRequest数据。 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportSummaryRequest {
    private String bbkId;
    private String orgId;
    private String level;
    @Builder.Default
    private List<String> skillIds = Lists.newArrayList();
    @NotNull(message = "日期不能为空")
    private LocalDate startDate;
    @NotNull(message = "日期不能为空")
    private LocalDate endDate;
    /** 返回集合快照，避免暴露内部可变集合。 */
    public List<String> getSkillIds() {
        return Collections.unmodifiableList(skillIds == null ? Lists.newArrayList() : Lists.newArrayList(skillIds));
    }

    /** 空字符串与未传参数保持一致。 */
    public String getBbkId() {
        return bbkId != null && bbkId.isEmpty() ? null : bbkId;
    }

    /** 空字符串与未传参数保持一致。 */
    public String getOrgId() {
        return orgId != null && orgId.isEmpty() ? null : orgId;
    }

    /** 空字符串与未传参数保持一致。 */
    public String getLevel() {
        return level != null && level.isEmpty() ? null : level;
    }
}
