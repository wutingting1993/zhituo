package com.cmbchina.domain.report.model;
import java.util.List;
import java.util.Collections;
import com.google.common.collect.Lists;
import lombok.Builder;
import lombok.Getter;
/** 单次请求加载的执行数据快照。 */
@Getter
@Builder
public class ExecutionDataset {
    private final List<ReportExecution> executions;
    private final List<ReportRecommendation> recommendations;
    private final List<ReportCustomerAction> actions;

    /** 返回执行快照。 */
    public List<ReportExecution> getExecutions() {
        return Collections.unmodifiableList(Lists.newArrayList(executions));
    }

    /** 返回推荐快照。 */
    public List<ReportRecommendation> getRecommendations() {
        return Collections.unmodifiableList(Lists.newArrayList(recommendations));
    }

    /** 返回行为快照。 */
    public List<ReportCustomerAction> getActions() {
        return Collections.unmodifiableList(Lists.newArrayList(actions));
    }
}
