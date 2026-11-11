package com.cmbchina.adapter.report.web;
import com.cmbchina.application.api.dto.Result;
import com.cmbchina.application.report.api.ReportApplicationService;
import com.cmbchina.application.report.api.dto.ReportFilterRequest;
import com.cmbchina.application.report.api.dto.ReportFiltersResponse;
import com.cmbchina.application.report.api.dto.ReportSummaryRequest;
import com.cmbchina.application.report.api.dto.ReportSummaryResponse;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
/** 报表只读接口。 */
@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {
    @Autowired
    private ReportApplicationService service;
    /** 获取检索选项。 */
    @GetMapping("/filters")
    public Result<ReportFiltersResponse> filters(@Valid @ModelAttribute ReportFilterRequest request) {
        return Result.success(service.filters(request));
    }

    /** 获取顶部统计指标。 */
    @GetMapping("/summary")
    public Result<ReportSummaryResponse> summary(@Valid @ModelAttribute ReportSummaryRequest request) {
        return Result.success(service.summary(request));
    }
}
