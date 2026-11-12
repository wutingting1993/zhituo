package com.cmbchina.application.report.api.dto;
import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
/** 关注支行总量及前四项。 */
@Getter
@Setter
@Builder
public class AttentionBranchesResponse {
    private long totalCount;
    @Builder.Default
    private List<AttentionBranchRow> rows = Lists.newArrayList();
    /** 返回关注支行快照。 */
    public List<AttentionBranchRow> getRows() {
        return Collections.unmodifiableList(Lists.newArrayList(rows));
    }
}
