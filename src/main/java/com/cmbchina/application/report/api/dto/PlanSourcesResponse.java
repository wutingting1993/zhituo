package com.cmbchina.application.report.api.dto;
import java.util.List;
import java.util.Collections;
import com.google.common.collect.Lists;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
/** 规划来源排名响应。 */
@Getter
@Setter
@Builder
public class PlanSourcesResponse {
    private List<PlanSourceRow> branches;
    /** 返回分行列表快照。 */
    public List<PlanSourceRow> getBranches() {
        return Collections.unmodifiableList(Lists.newArrayList(branches));
    }
}
