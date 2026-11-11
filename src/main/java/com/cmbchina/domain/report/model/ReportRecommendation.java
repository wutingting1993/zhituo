package com.cmbchina.domain.report.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/** 报表ReportRecommendation数据。 */
@Getter
@Setter
@Builder
public class ReportRecommendation {
    private ReportExecution execution;
    private String customerId;
}
