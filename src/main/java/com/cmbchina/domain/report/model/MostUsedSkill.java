package com.cmbchina.domain.report.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/** 报表MostUsedSkill数据。 */
@Getter
@Setter
@Builder
public class MostUsedSkill {
    private String id;
    private String name;
    private long userCount;
}
