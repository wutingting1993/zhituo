package com.cmbchina.application.monitor.api.dto;

import java.time.LocalDate;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** 接收面向监控页面的日期和业务筛选。 */
@Getter
@Setter
public class MonitorQuery {
    @NotNull
    private LocalDate startDate;
    @NotNull
    private LocalDate endDate;
    private String interfaceId;
    private String agentId;
    private boolean unattributedOnly;
}
