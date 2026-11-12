package com.cmbchina.application.report.api.dto;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

/** 技能执行明细及同入口分类名称。 */
@Getter
@Setter
@Builder
public class ExecutionSkillRow {
    private String skillId;
    private String skillName;
    private String categoryName;
    private long managerCount;
    private long taskCount;
    private long scheduledTaskCount;
    private long executedTaskCount;
    private long pendingTaskCount;
    private long failedTaskCount;
    private long recommendedCustomerCount;
    private BigDecimal viewRate;
    private BigDecimal contactRate;
    private long planCount;

}
