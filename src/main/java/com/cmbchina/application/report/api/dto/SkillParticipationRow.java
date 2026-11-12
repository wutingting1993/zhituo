package com.cmbchina.application.report.api.dto;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
/** 技能内人员去重后的参与情况。 */
@Getter
@Setter
@Builder
public class SkillParticipationRow {
    private String skillId;
    private String skillName;
    private long participantCount;
    private long assignedManagerCount;
    private BigDecimal participationRate;
}
