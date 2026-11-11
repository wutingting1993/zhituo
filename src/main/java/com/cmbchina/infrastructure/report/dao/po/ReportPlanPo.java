package com.cmbchina.infrastructure.report.dao.po;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/** 规划主表与负责人查询记录。 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportPlanPo {
    private String id;
    private String name;
    private String source;
    private String ownerId;
    private String ownerName;
    private String ownerLevel;
    private String periodStart;
    private String periodEnd;
}
