package com.cmbchina.domain.monitor.support;

import com.cmbchina.domain.monitor.model.MonitorCoverage;
import com.cmbchina.domain.monitor.model.MonitorBatchMetric;
import com.cmbchina.domain.monitor.model.MonitorFilter;
import com.cmbchina.domain.monitor.model.MonitorMetricRow;
import com.cmbchina.domain.monitor.model.MonitorRange;
import com.cmbchina.domain.monitor.model.MonitorRefreshCommand;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 监控统计和名称数据来源。 */
public interface MonitorDataSupport {

    /** 获取已发布的统计记录。 */
    List<MonitorMetricRow> queryStatistics(MonitorFilter request);

    /** 查询批量入口的分钟指标或区间汇总。 */
    List<MonitorBatchMetric> queryBatchStatistics(MonitorFilter request);

    /** 获取离线统计覆盖记录。 */
    List<MonitorCoverage> queryCoverage(MonitorRange request);

    /** 调用离线统计更新接口。 */
    void refresh(MonitorRefreshCommand request);

    /** 查询当前维度的名称。 */
    Map<String, String> names(String group, Set<String> ids);

    /** 查询接口当前已发布版本的缓存开关，未关联配置时返回空。 */
    Boolean cacheEnabled(String interfaceId);
}
