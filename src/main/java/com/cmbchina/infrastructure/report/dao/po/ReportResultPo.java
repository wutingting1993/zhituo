package com.cmbchina.infrastructure.report.dao.po;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/** 报表ReportResultPo数据。 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportResultPo {
    private String jobId;
    private String skillId;
    private String traceId;
    private String customerId;
}
