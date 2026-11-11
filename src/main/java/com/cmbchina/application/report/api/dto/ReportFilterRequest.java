package com.cmbchina.application.report.api.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.util.List;
import java.util.Collections;
import com.google.common.collect.Lists;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
/** 报表ReportFilterRequest数据。 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportFilterRequest {
    private String bbkId;
    private String orgId;
    @Builder.Default
    private List<String> skillIds = Lists.newArrayList();
    /** 返回集合快照，避免暴露内部可变集合。 */
    public List<String> getSkillIds() {
        return Collections.unmodifiableList(skillIds == null ? Lists.newArrayList() : Lists.newArrayList(skillIds));
    }
}
