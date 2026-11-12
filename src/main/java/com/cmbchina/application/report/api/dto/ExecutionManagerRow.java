package com.cmbchina.application.report.api.dto;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

import java.util.List;
import java.util.Collections;
import com.google.common.collect.Lists;
/** 客户经理执行明细。 */
@Getter
@Setter
@Builder
public class ExecutionManagerRow {
    private String sapId;
    private String userName;
    private String level;
    @Builder.Default
    private List<ExecutionSkillOption> skills = Lists.newArrayList();
    private long taskCount;
    private long recommendedCustomerCount;
    private BigDecimal viewRate;
    private BigDecimal insightRate;
    private BigDecimal callRate;
    private BigDecimal contactRate;
    /** 返回集合快照。 */
    public List<ExecutionSkillOption> getSkills() {
        return Collections.unmodifiableList(Lists.newArrayList(skills));
    }

}
