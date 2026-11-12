package com.cmbchina.application.report.api.dto;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Collections;
import com.google.common.collect.Lists;
/** 支行客户经理下钻响应。 */
@Getter
@Setter
@Builder
public class ExecutionManagerDetailsResponse {
    private String bbkId;
    private String bbkName;
    private String orgId;
    private String orgName;
    private ExecutionManagerSummary summary;
    @Builder.Default
    private List<ExecutionManagerRow> rows = Lists.newArrayList();
    /** 返回集合快照。 */
    public List<ExecutionManagerRow> getRows() {
        return Collections.unmodifiableList(Lists.newArrayList(rows));
    }

}
