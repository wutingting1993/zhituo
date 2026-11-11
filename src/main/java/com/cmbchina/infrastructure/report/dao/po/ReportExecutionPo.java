package com.cmbchina.infrastructure.report.dao.po;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
/** 报表ReportExecutionPo数据。 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportExecutionPo {
    private String jobId;
    private String traceId;
    private LocalDateTime actualTime;
    private boolean read;
}
