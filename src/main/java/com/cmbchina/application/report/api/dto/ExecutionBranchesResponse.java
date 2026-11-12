package com.cmbchina.application.report.api.dto;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Collections;
import com.google.common.collect.Lists;
/** 支行执行列表。 */
@Getter
@Setter
@Builder
public class ExecutionBranchesResponse {
    private ExecutionBenchmarkResponse benchmark;
    @Builder.Default
    private List<ExecutionBranchRow> rows = Lists.newArrayList();
    /** 返回集合快照。 */
    public List<ExecutionBranchRow> getRows() {
        return Collections.unmodifiableList(Lists.newArrayList(rows));
    }

}
