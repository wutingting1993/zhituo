package com.cmbchina.application.report.api.dto;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/** 分行及支行规划覆盖响应。 */
@Getter
@Setter
@Builder
public class PlanCoverageResponse {
    private java.util.List<PlanCoverageRow> branches;
    /** 返回列表快照。 */
    public java.util.List<PlanCoverageRow> getBranches() {
        return java.util.Collections.unmodifiableList(com.google.common.collect.Lists.newArrayList(branches));
    }
}
