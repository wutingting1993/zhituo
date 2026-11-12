package com.cmbchina.domain.report.service;
import com.cmbchina.domain.report.model.ExecutionMetrics;
import com.cmbchina.domain.report.model.ReportContact;
import com.cmbchina.domain.report.model.ReportCustomerAction;
import com.cmbchina.domain.report.model.ReportExecution;
import com.cmbchina.domain.report.model.ReportRecommendation;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
/** 执行阶段分别按客户去重，不强制逐级包含。 */
@Service
public class ExecutionStatisticsServiceImpl implements ExecutionStatisticsService {
    @Autowired
    private ReportStatisticsService statistics;

    @Override
    public ExecutionMetrics summarize(List<ReportExecution> executions, List<ReportRecommendation> recommendations,
            List<ReportCustomerAction> actions) {
        List<ReportContact> contacts = actions.stream().map(a -> ReportContact.builder().customerId(a.getContactCustomerId()).build())
                .collect(Collectors.toList());
        return ExecutionMetrics.builder().statistics(statistics.summarize(executions, recommendations, contacts))
                .insightCustomerCount(customers(actions, "insight")).callCustomerCount(customers(actions, "phone")).build();
    }

    private long customers(List<ReportCustomerAction> actions, String buttonType) {
        return actions.stream().filter(a -> buttonType.equals(a.getButtonType())).map(a -> a.getRecommendation().getCustomerId())
                .filter(id -> !id.isEmpty()).distinct().count();
    }
}
