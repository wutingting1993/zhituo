package com.cmbchina.application.report.api;
import com.cmbchina.application.report.api.dto.ExecutionBranchesRequest;
import com.cmbchina.application.report.api.dto.ExecutionBranchesResponse;
import com.cmbchina.application.report.api.dto.ExecutionDetailsRequest;
import com.cmbchina.application.report.api.dto.ExecutionManagerDetailsResponse;
import com.cmbchina.application.report.api.dto.ExecutionOverviewResponse;
import com.cmbchina.application.report.api.dto.ExecutionSkillDetailsResponse;
import com.cmbchina.application.report.api.dto.ReportSummaryRequest;
import com.cmbchina.application.report.api.dto.ExecutionTrendResponse;
import com.cmbchina.application.report.api.dto.SkillParticipationRequest;
import com.cmbchina.application.report.api.dto.SkillParticipationResponse;
import com.cmbchina.application.report.api.dto.AttentionBranchesResponse;
/** 执行视角报表用例。 */
public interface ExecutionReportApplicationService {
    /** 获取所选区间每日执行走势。 */
    ExecutionTrendResponse trend(ReportSummaryRequest request);

    /** 获取按指定指标排序的技能参与度。 */
    SkillParticipationResponse participation(SkillParticipationRequest request);

    /** 获取使用或接触不足的支行。 */
    AttentionBranchesResponse attention(ReportSummaryRequest request);

    /** 获取执行链路概览。 */
    ExecutionOverviewResponse overview(ReportSummaryRequest request);

    /** 获取支行执行列表。 */
    ExecutionBranchesResponse branches(ExecutionBranchesRequest request);

    /** 获取支行客户经理明细。 */
    ExecutionManagerDetailsResponse managers(ExecutionDetailsRequest request);

    /** 获取支行技能明细。 */
    ExecutionSkillDetailsResponse skills(ExecutionDetailsRequest request);
}
