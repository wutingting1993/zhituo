package com.cmbchina.application.report.api.dto;
import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
/** 技能参与度前N项。 */
@Getter
@Setter
@Builder
public class SkillParticipationResponse {
    @Builder.Default
    private List<SkillParticipationRow> rows = Lists.newArrayList();
    /** 返回排名快照。 */
    public List<SkillParticipationRow> getRows() {
        return Collections.unmodifiableList(Lists.newArrayList(rows));
    }
}
