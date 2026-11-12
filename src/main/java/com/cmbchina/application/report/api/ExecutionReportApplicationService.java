package com.cmbchina.application.report.api;
import com.cmbchina.application.report.api.dto.ExecutionBranchesRequest;
import com.cmbchina.application.report.api.dto.ExecutionBranchesResponse;
import com.cmbchina.application.report.api.dto.ExecutionDetailsRequest;
import com.cmbchina.application.report.api.dto.ExecutionManagerDetailsResponse;
import com.cmbchina.application.report.api.dto.ExecutionOverviewResponse;
import com.cmbchina.application.report.api.dto.ExecutionSkillDetailsResponse;
import com.cmbchina.application.report.api.dto.ReportSummaryRequest;
/** 执行视角报表用例。 */
public interface ExecutionReportApplicationService {
    /** 获取执行链路概览。 */
    ExecutionOverviewResponse overview(ReportSummaryRequest request);

    /** 获取支行执行列表。 */
    ExecutionBranchesResponse branches(ExecutionBranchesRequest request);

    /** 获取支行客户经理明细。 */
    ExecutionManagerDetailsResponse managers(ExecutionDetailsRequest request);

    /** 获取支行技能明细。 */
    ExecutionSkillDetailsResponse skills(ExecutionDetailsRequest request);
}
