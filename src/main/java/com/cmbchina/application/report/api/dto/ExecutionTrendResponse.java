package com.cmbchina.application.report.api.dto;
import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
/** 所选区间完整逐日走势。 */
@Getter
@Setter
@Builder
public class ExecutionTrendResponse {
    @Builder.Default
    private List<ExecutionTrendPoint> dailyPoints = Lists.newArrayList();
    /** 返回每日数据快照。 */
    public List<ExecutionTrendPoint> getDailyPoints() {
        return Collections.unmodifiableList(Lists.newArrayList(dailyPoints));
    }
}
