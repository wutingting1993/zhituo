package com.cmbchina.adapter.report.web;
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
