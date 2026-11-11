package com.cmbchina.application.report.api;
import com.cmbchina.application.report.api.dto.PlanCoverageResponse;
import com.cmbchina.application.report.api.dto.PlanDetailsRequest;
import com.cmbchina.application.report.api.dto.PlanDetailsResponse;
import com.cmbchina.application.report.api.dto.ReportSummaryRequest;
/** 规划覆盖及机构下钻用例契约。 */
public interface PlanCoverageApplicationService {
    /** 获取分支行覆盖。 */
    PlanCoverageResponse coverage(ReportSummaryRequest request);

    /** 获取指定机构的规划明细。 */
    PlanDetailsResponse details(PlanDetailsRequest request);
}
