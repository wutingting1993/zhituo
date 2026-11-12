package com.cmbchina.domain.report.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.time.LocalDate;
/** 报表ReportExecution数据。 */
@Getter
@Setter
@Builder
public class ReportExecution {
    private ReportRelation relation;
    private String traceId;
    private LocalDateTime actualTime;
    private LocalDate executionDay;
    private boolean read;
    private String taskStatus;
}
