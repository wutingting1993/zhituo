package com.cmbchina.application.monitor.api;

import com.cmbchina.application.monitor.api.dto.MonitorQuery;
import com.cmbchina.application.monitor.api.dto.MonitorRefresh;
import com.cmbchina.application.monitor.api.dto.MonitorQueryResponse;

/** 配置接口监控的用例入口。 */
public interface MonitorApplicationService {

    /** 查询全局总览。 */
    MonitorQueryResponse overview(MonitorQuery request);

    /** 查询接口指标列表。 */
    MonitorQueryResponse interfaces(MonitorQuery request);

    /** 查询专家指标列表。 */
    MonitorQueryResponse experts(MonitorQuery request);

    /** 查询详情汇总。 */
    MonitorQueryResponse summary(MonitorQuery request);

    /** 查询详情趋势。 */
    MonitorQueryResponse trend(MonitorQuery request);

    /** 触发指定周期的离线统计更新。 */
    void refresh(MonitorRefresh request);
}
