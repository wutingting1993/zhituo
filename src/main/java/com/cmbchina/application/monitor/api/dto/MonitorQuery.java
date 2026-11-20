package com.cmbchina.application.monitor.api.dto;

import java.time.LocalDate;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** 接收面向监控页面的日期筛选与分组。 */
@Getter
@Setter
public class MonitorQuery {
    @NotNull
    private LocalDate startDate;
    @NotNull
    private LocalDate endDate;
    @NotBlank
    private String groupBy;
    private String interfaceId;
    private String agentId;
    private boolean unattributedOnly;
    private int bucketSeconds;
}
