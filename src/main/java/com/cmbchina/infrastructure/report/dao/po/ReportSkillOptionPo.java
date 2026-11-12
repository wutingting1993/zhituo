package com.cmbchina.infrastructure.report.dao.po;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
/** 技能选项编号与名称。 */
@Getter
@Setter
@Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class ReportSkillOptionPo {
    private String id;
    private String name;
}
