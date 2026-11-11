package com.cmbchina.domain.report.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.util.List;
import java.util.Collections;
import com.google.common.collect.Lists;
import java.time.LocalDate;
/** 报表ReportQuery数据。 */
@Getter
@Setter
@Builder
public class ReportQuery {
    private String bbkId;
    private String orgId;
    private String level;
    @Builder.Default
    private List<String> skillIds = Lists.newArrayList();
    private LocalDate startDate;
    private LocalDate endDate;
    /** 返回集合快照，避免暴露内部可变集合。 */
    public List<String> getSkillIds() {
        return Collections.unmodifiableList(skillIds == null ? Lists.newArrayList() : Lists.newArrayList(skillIds));
    }
}
