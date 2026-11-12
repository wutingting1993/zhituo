package com.cmbchina.application.report.api.dto;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/** 支行技能汇总。 */
@Getter
@Setter
@Builder
public class ExecutionSkillSummary {
    private long skillCount;
    private long managerCount;
    private long taskCount;
    private long planCount;

}
