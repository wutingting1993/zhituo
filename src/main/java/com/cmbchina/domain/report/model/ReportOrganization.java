package com.cmbchina.domain.report.model;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
/** 分支行编号与展示名称。 */
@Getter
@Setter
@Builder
public class ReportOrganization {
    private String bbkId;
    private String bbkName;
    private String orgId;
    private String orgName;
}
