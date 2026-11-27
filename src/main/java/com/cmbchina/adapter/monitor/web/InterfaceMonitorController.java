package com.cmbchina.adapter.monitor.web;

import com.cmbchina.application.api.dto.Result;
import com.cmbchina.application.monitor.api.MonitorApplicationService;
import com.cmbchina.application.monitor.api.dto.MonitorQuery;
import com.cmbchina.application.monitor.api.dto.MonitorBatchResponse;
import com.cmbchina.application.monitor.api.dto.MonitorRefresh;
import com.cmbchina.application.monitor.api.dto.MonitorQueryResponse;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 提供配置接口监控的页面查询与手动更新入口。 */
@RestController
@RequestMapping("/api/v1/interface-monitor")
public class InterfaceMonitorController {
    @Autowired
    private MonitorApplicationService service;

    /** 查询全局总览。 */
    @PostMapping("/overview")
    public Result<MonitorQueryResponse> overview(@Valid @RequestBody MonitorQuery request) {
        return Result.success(service.overview(request));
    }

    /** 查询接口指标列表。 */
    @PostMapping("/interfaces")
    public Result<MonitorQueryResponse> interfaces(@Valid @RequestBody MonitorQuery request) {
        return Result.success(service.interfaces(request));
    }

    /** 查询专家指标列表。 */
    @PostMapping("/experts")
    public Result<MonitorQueryResponse> experts(@Valid @RequestBody MonitorQuery request) {
        return Result.success(service.experts(request));
    }

    /** 查询详情汇总。 */
    @PostMapping("/summary")
    public Result<MonitorQueryResponse> summary(@Valid @RequestBody MonitorQuery request) {
        return Result.success(service.summary(request));
    }

    /** 查询详情趋势。 */
    @PostMapping("/trend")
    public Result<MonitorQueryResponse> trend(@Valid @RequestBody MonitorQuery request) {
        return Result.success(service.trend(request));
    }

    /** 查询仅按专家和日期限定的批量请求指标。 */
    @PostMapping("/batch-metrics")
    public Result<MonitorBatchResponse> batchMetrics(@Valid @RequestBody MonitorQuery request) {
        return Result.success(service.batchMetrics(request));
    }

    /** 将手动更新请求交给现有离线任务接口。 */
    @PostMapping("/refresh")
    public Result<Void> refresh(@Valid @RequestBody MonitorRefresh request) {
        service.refresh(request);
        return Result.success(null);
    }
}
