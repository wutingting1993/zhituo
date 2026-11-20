package com.cmbchina.application.monitor.api;

import com.cmbchina.application.monitor.api.dto.MonitorQuery;
import com.cmbchina.application.monitor.api.dto.MonitorRefresh;
import com.fasterxml.jackson.databind.JsonNode;

/** 配置接口监控的用例入口。 */
public interface MonitorApplicationService {

    /** 查询全局总览。 */
    JsonNode overview(MonitorQuery request);

    /** 查询接口指标列表。 */
    JsonNode interfaces(MonitorQuery request);

    /** 查询专家指标列表。 */
    JsonNode experts(MonitorQuery request);

    /** 查询详情汇总。 */
    JsonNode summary(MonitorQuery request);

    /** 查询详情趋势。 */
    JsonNode trend(MonitorQuery request);

    /** 触发指定周期的离线统计更新。 */
    JsonNode refresh(MonitorRefresh request);
}
