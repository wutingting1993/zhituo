package com.cmbchina.infrastructure.report.dao.po;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/** 点击事件及接触关联的持久化结果。 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportActionPo {
    private String traceId;
    private String customerId;
    private java.time.LocalDate executionDay;
    private String buttonType;
    private String contactCustomerId;

}
