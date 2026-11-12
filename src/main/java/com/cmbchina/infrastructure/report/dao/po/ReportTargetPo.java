package com.cmbchina.infrastructure.report.dao.po;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
/** 报表ReportTargetPo数据。 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportTargetPo {
    private String bbkId;
    private String bbkName;
    private String orgId;
    private String orgName;
    private String level;
    private String sapId;
    private String userName;
    private String planId;
    private LocalDateTime createdAt;
}
