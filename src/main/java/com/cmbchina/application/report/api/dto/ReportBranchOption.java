package com.cmbchina.application.report.api.dto;
import lombok.Builder;
import lombok.Getter;
import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
/** 分行及所属支行选项。 */
@Getter
@Builder
public class ReportBranchOption {
    private String id;
    private String name;
    @Builder.Default
    private List<ReportOption> subBranches = Lists.newArrayList();
    /** 返回支行快照。 */
    public List<ReportOption> getSubBranches() {
        return Collections.unmodifiableList(Lists.newArrayList(subBranches));
    }
}
