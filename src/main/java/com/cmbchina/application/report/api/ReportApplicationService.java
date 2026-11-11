package com.cmbchina.application.report.api;
import com.cmbchina.application.report.api.dto.ReportFilterRequest;
import com.cmbchina.application.report.api.dto.ReportFiltersResponse;
import com.cmbchina.application.report.api.dto.ReportSummaryRequest;
import com.cmbchina.application.report.api.dto.ReportSummaryResponse;
/** 报表查询用例。 */
public interface ReportApplicationService {
    /** 获取联动选项。 */
    ReportFiltersResponse filters(ReportFilterRequest request);

    /** 获取指标及辅助数据。 */
    ReportSummaryResponse summary(ReportSummaryRequest request);
}
