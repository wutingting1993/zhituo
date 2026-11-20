package com.cmbchina.adapter.monitor.web;

import com.cmbchina.application.api.dto.Result;
import com.cmbchina.application.monitor.api.MonitorApplicationService;
import com.cmbchina.application.monitor.api.dto.MonitorQuery;
import com.cmbchina.application.monitor.api.dto.MonitorRefresh;
import com.fasterxml.jackson.databind.JsonNode;
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

    /** 查询所选日期范围的离线统计。 */
    @PostMapping("/query")
    public Result<JsonNode> query(@Valid @RequestBody MonitorQuery request) {
        return Result.success(service.query(request));
    }

    /** 将手动更新请求交给现有离线任务接口。 */
    @PostMapping("/refresh")
    public Result<JsonNode> refresh(@Valid @RequestBody MonitorRefresh request) {
        return Result.success(service.refresh(request));
    }
}
