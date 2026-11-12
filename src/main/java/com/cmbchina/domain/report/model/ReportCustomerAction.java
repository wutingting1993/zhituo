package com.cmbchina.domain.report.model;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/** 推荐客户的按钮行为及关联接触客户。 */
@Getter
@Setter
@Builder
public class ReportCustomerAction {
    private ReportRecommendation recommendation;
    private String buttonType;
    private String contactCustomerId;

}
