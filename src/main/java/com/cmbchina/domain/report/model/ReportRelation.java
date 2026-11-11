package com.cmbchina.domain.report.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
/** 报表ReportRelation数据。 */
@Getter
@Setter
@Builder
public class ReportRelation {
    private String bbkId;
    private String bbkName;
    private String orgId;
    private String orgName;
    private String level;
    private String sapId;
    private String planId;
    private LocalDateTime createdAt;
    private String skillId;
    private String skillName;
    private String jobId;
}
