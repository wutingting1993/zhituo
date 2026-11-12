package com.cmbchina.infrastructure.report.dao.po;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
/** 规划与场景技能匹配键。 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportPlanSkillPo {
    private String planId;
    private String skillId;
}
