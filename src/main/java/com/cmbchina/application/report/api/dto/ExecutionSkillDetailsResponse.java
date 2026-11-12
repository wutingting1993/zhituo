package com.cmbchina.application.report.api.dto;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Collections;
import com.google.common.collect.Lists;
/** 支行技能下钻响应。 */
@Getter
@Setter
@Builder
public class ExecutionSkillDetailsResponse {
    private String bbkId;
    private String bbkName;
    private String orgId;
    private String orgName;
    private ExecutionSkillSummary summary;
    @Builder.Default
    private List<ExecutionSkillRow> rows = Lists.newArrayList();
    /** 返回集合快照。 */
    public List<ExecutionSkillRow> getRows() {
        return Collections.unmodifiableList(Lists.newArrayList(rows));
    }

}
