package com.cmbchina.domain.report.model;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
/** 技能选项编号与名称。 */
@Getter
@Setter
@Builder
public class ReportSkillOption {
    private String id;
    private String name;
}
