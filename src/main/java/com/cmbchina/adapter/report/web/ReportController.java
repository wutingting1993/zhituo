package com.cmbchina.adapter.report.web;
import com.cmbchina.application.report.api.ExecutionReportApplicationService;
import com.cmbchina.application.report.api.dto.ExecutionBranchesRequest;
import com.cmbchina.application.report.api.dto.ExecutionBranchesResponse;
import com.cmbchina.application.report.api.dto.ExecutionDetailsRequest;
import com.cmbchina.application.report.api.dto.ExecutionManagerDetailsResponse;
import com.cmbchina.application.report.api.dto.ExecutionOverviewResponse;
import com.cmbchina.application.report.api.dto.ExecutionSkillDetailsResponse;
import com.cmbchina.application.report.api.PlanCoverageApplicationService;
import com.cmbchina.application.report.api.dto.PlanCoverageResponse;
import com.cmbchina.application.report.api.dto.PlanDetailsRequest;
import com.cmbchina.application.report.api.dto.PlanDetailsResponse;
import com.cmbchina.application.api.dto.Result;
import com.cmbchina.application.report.api.ReportApplicationService;
import com.cmbchina.application.report.api.dto.ReportFilterRequest;
import com.cmbchina.application.report.api.dto.ReportFiltersResponse;
import com.cmbchina.application.report.api.dto.ReportSummaryRequest;
import com.cmbchina.application.report.api.dto.ReportSummaryResponse;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
/** 报表只读接口。 */
@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {
    @Autowired
    private ReportApplicationService service;
    @Autowired
    private PlanCoverageApplicationService coverage;

    @Autowired
    private ExecutionReportApplicationService execution;

    /** 获取执行链路概览。 */
    @PostMapping("/execution-overview")
    public Result<ExecutionOverviewResponse> executionOverview(@Valid @RequestBody ReportSummaryRequest request) {
        return Result.success(execution.overview(request));
    }

    /** 获取支行执行明细。 */
    @PostMapping("/execution-branches")
    public Result<ExecutionBranchesResponse> executionBranches(@Valid @RequestBody ExecutionBranchesRequest request) {
        return Result.success(execution.branches(request));
    }

    /** 获取支行客户经理执行明细。 */
    @PostMapping("/execution-branches/managers")
    public Result<ExecutionManagerDetailsResponse> executionManagers(@Valid @RequestBody ExecutionDetailsRequest request) {
        return Result.success(execution.managers(request));
    }

    /** 获取支行技能执行明细。 */
    @PostMapping("/execution-branches/skills")
    public Result<ExecutionSkillDetailsResponse> executionSkills(@Valid @RequestBody ExecutionDetailsRequest request) {
        return Result.success(execution.skills(request));
    }

    /** 获取分支行规划覆盖。 */
    @PostMapping("/plan-coverage")
    public Result<PlanCoverageResponse> coverage(@Valid @RequestBody ReportSummaryRequest request) {
        return Result.success(coverage.coverage(request));
    }

    /** 获取机构内规划明细。 */
    @PostMapping("/plan-coverage/details")
    public Result<PlanDetailsResponse> details(@Valid @RequestBody PlanDetailsRequest request) {
        return Result.success(coverage.details(request));
    }

    /** 获取检索选项。 */
    @PostMapping("/filters")
    public Result<ReportFiltersResponse> filters(@Valid @RequestBody ReportFilterRequest request) {
        return Result.success(service.filters(request));
    }

    /** 获取顶部统计指标。 */
    @PostMapping("/summary")
    public Result<ReportSummaryResponse> summary(@Valid @RequestBody ReportSummaryRequest request) {
        return Result.success(service.summary(request));
    }
}
