package com.cmbchina.application.report.api.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.util.List;
import java.util.Collections;
import com.google.common.collect.Lists;
/** 报表ReportFiltersResponse数据。 */
@Getter
@Setter
@Builder
public class ReportFiltersResponse {
    @Builder.Default
    private List<ReportOption> branches = Lists.newArrayList();
    @Builder.Default
    private List<ReportOption> subBranches = Lists.newArrayList();
    @Builder.Default
    private List<ReportOption> skills = Lists.newArrayList();
    @Builder.Default
    private List<String> levels = Lists.newArrayList();
    /** 返回集合快照，避免暴露内部可变集合。 */
    public List<ReportOption> getBranches() {
        return Collections.unmodifiableList(branches == null ? Lists.newArrayList() : Lists.newArrayList(branches));
    }

    /** 返回集合快照，避免暴露内部可变集合。 */
    public List<ReportOption> getSubBranches() {
        return Collections.unmodifiableList(subBranches == null ? Lists.newArrayList() : Lists.newArrayList(subBranches));
    }

    /** 返回集合快照，避免暴露内部可变集合。 */
    public List<ReportOption> getSkills() {
        return Collections.unmodifiableList(skills == null ? Lists.newArrayList() : Lists.newArrayList(skills));
    }

    /** 返回集合快照，避免暴露内部可变集合。 */
    public List<String> getLevels() {
        return Collections.unmodifiableList(levels == null ? Lists.newArrayList() : Lists.newArrayList(levels));
    }
}
