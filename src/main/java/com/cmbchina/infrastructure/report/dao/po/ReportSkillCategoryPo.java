package com.cmbchina.infrastructure.report.dao.po;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
/** 技能分类名称查询结果。 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportSkillCategoryPo {
    private String skillId;
    private String categoryName;
}
