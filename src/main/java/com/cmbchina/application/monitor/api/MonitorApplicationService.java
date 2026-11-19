package com.cmbchina.application.monitor.api;

import com.cmbchina.application.monitor.api.dto.MonitorQuery;
import com.cmbchina.application.monitor.api.dto.MonitorRefresh;
import com.fasterxml.jackson.databind.JsonNode;

/** 配置接口监控的用例入口。 */
public interface MonitorApplicationService {

    /** 查询已经发布的离线监控统计。 */
    JsonNode query(MonitorQuery request);

    /** 触发指定周期的离线统计更新。 */
    JsonNode refresh(MonitorRefresh request);
}
