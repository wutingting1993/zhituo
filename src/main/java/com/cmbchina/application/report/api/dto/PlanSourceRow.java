package com.cmbchina.application.report.api.dto;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
/** 分行规划来源数量。 */
@Getter
@Setter
@Builder
public class PlanSourceRow {
    private String id;
    private String name;
    private long planCount;
    private long myCount;
    private long branchCount;
    private long presidentCount;
}
