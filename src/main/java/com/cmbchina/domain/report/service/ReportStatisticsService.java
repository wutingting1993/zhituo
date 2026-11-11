package com.cmbchina.domain.report.service;
import com.cmbchina.domain.report.model.ReportExecution;
import com.cmbchina.domain.report.model.ReportRecommendation;
import com.cmbchina.domain.report.model.ReportContact;
import com.cmbchina.domain.report.model.ReportStatistics;
import java.util.List;
/** 全区间去重统计契约。 */
public interface ReportStatisticsService {
    /** 汇总本次范围内全部记录。 */
    ReportStatistics summarize(List<ReportExecution> executions, List<ReportRecommendation> recommendations, List<ReportContact> contacts);
}
