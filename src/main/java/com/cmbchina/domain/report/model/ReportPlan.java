package com.cmbchina.domain.report.model;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/** 规划来源、负责人及展示信息。 */
@Getter
@Setter
@Builder
public class ReportPlan {
    private String id;
    private String name;
    private String source;
    private String ownerId;
    private String ownerName;
    private String ownerLevel;
    private String periodStart;
    private String periodEnd;
}
