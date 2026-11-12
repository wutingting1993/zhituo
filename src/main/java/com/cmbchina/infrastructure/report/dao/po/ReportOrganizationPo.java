package com.cmbchina.infrastructure.report.dao.po;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
/** 分支行编号与展示名称。 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportOrganizationPo {
    private String bbkId;
    private String bbkName;
    private String orgId;
    private String orgName;
}
