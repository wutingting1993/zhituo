package com.cmbchina.application.monitor.api.dto;

import javax.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/** 指定离线更新的周期和向历史回退的偏移。 */
@Getter
@Setter
public class MonitorRefresh {
    @NotBlank
    private String periodType;
    private long offset;
}
