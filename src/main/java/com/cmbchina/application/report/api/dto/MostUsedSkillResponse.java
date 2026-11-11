package com.cmbchina.application.report.api.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/** 报表MostUsedSkillResponse数据。 */
@Getter
@Setter
@Builder
public class MostUsedSkillResponse {
    private String id;
    private String name;
    private long userCount;
}
