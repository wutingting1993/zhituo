package com.cmbchina.domain.report.model;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
/** 一个数据库分组的汇总及展示维度。 */
@Getter
@Setter
@Builder
public class ReportAggregate {
    private String key;
    private String secondKey;
    private LocalDate day;
    private ReportRelation relation;
    private ExecutionMetrics metrics;
    private long myCount;
    private long branchCount;
    private long presidentCount;
    private long assignedManagerCount;
    private long participantCount;
}
