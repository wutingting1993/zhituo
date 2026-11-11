package com.cmbchina.infrastructure.report.dao.po;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/** 报表ReportJobPo数据。 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportJobPo {
    private String planId;
    private String sapId;
    private String skillId;
    private String skillName;
    private String jobId;
}
