package com.cmbchina.application.report.api.dto;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/** 客户经理参与执行的技能。 */
@Getter
@Setter
@Builder
public class ExecutionSkillOption {
    private String skillId;
    private String skillName;

}
