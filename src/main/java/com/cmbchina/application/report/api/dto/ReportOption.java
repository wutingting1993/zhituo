package com.cmbchina.application.report.api.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/** 报表ReportOption数据。 */
@Getter
@Setter
@Builder
public class ReportOption {
    private String id;
    private String name;
}
