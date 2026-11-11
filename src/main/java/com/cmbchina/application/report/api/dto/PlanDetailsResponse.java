package com.cmbchina.application.report.api.dto;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/** 机构规划明细及不受来源标签影响的汇总。 */
@Getter
@Setter
@Builder
public class PlanDetailsResponse {
    private String bbkId;
    private String orgId;
    private String name;
    private long planCount;
    private long recommendedCustomerCount;
    private long taskCount;
    private long skillCount;
    private long myCount;
    private long branchCount;
    private long presidentCount;
    private java.util.List<PlanDetailRow> plans;
    /** 返回列表快照。 */
    public java.util.List<PlanDetailRow> getPlans() {
        return java.util.Collections.unmodifiableList(com.google.common.collect.Lists.newArrayList(plans));
    }
}
