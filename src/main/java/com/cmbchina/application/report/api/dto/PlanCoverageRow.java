package com.cmbchina.application.report.api.dto;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/** 分支行规划来源数量及下属机构。 */
@Getter
@Setter
@Builder
public class PlanCoverageRow {
    private String id;
    private String name;
    private long myCount;
    private long branchCount;
    private long presidentCount;
    private long planCount;
    private long subBranchCount;
    private java.util.List<PlanCoverageRow> children;
    /** 返回列表快照。 */
    public java.util.List<PlanCoverageRow> getChildren() {
        return java.util.Collections.unmodifiableList(com.google.common.collect.Lists.newArrayList(children));
    }
}
