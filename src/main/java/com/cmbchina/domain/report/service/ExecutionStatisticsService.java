package com.cmbchina.domain.report.service;
import com.cmbchina.domain.report.model.ExecutionMetrics;
import com.cmbchina.domain.report.model.ReportCustomerAction;
import com.cmbchina.domain.report.model.ReportExecution;
import com.cmbchina.domain.report.model.ReportRecommendation;
import java.util.List;
/** 执行视角客户行为统计契约。 */
public interface ExecutionStatisticsService {
    /** 保留存量统计并对洞察、电访客户去重。 */
    ExecutionMetrics summarize(List<ReportExecution> executions, List<ReportRecommendation> recommendations, List<ReportCustomerAction> actions);
}
