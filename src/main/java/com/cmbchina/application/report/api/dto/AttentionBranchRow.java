package com.cmbchina.application.report.api.dto;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
/** 支行方案使用与接触诊断。 */
@Getter
@Setter
@Builder
public class AttentionBranchRow {
    private String bbkId;
    private String bbkName;
    private String orgId;
    private String orgName;
    private String diagnosis;
    private long recommendedCustomerCount;
    private long viewCustomerCount;
    private long contactCustomerCount;
    private BigDecimal viewRate;
    private BigDecimal contactRate;
    private BigDecimal benchmarkViewRate;
    private BigDecimal benchmarkContactRate;
    private BigDecimal viewRateDifference;
    private BigDecimal contactRateDifference;
}
