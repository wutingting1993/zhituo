package com.cmbchina.infrastructure.monitor.dao.mapper;

import com.cmbchina.domain.monitor.model.MonitorFilter;
import com.cmbchina.domain.monitor.model.MonitorBatchMetric;
import com.cmbchina.domain.monitor.model.MonitorRange;
import com.cmbchina.infrastructure.monitor.dao.po.MonitorCoverageRow;
import com.cmbchina.infrastructure.monitor.dao.po.MonitorStatisticsRow;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/** 通过独立的 ClickHouse 会话读取并更新接口监控统计。 */
public interface MonitorStatisticsMapper {
    List<MonitorStatisticsRow> queryStatistics(MonitorFilter filter);

    /** 获取批量请求的汇总或分钟指标。 */
    List<MonitorBatchMetric> queryBatchStatistics(MonitorFilter filter);

    /** 更新指定日期的批量请求分钟指标。 */
    void saveBatchMetrics(@Param("statDate") String statDate, @Param("coveredUntil") String coveredUntil,
            @Param("availableFrom") String availableFrom);

    /** 发布指定日期的批量请求分钟覆盖。 */
    void saveBatchCoverage(@Param("statDate") String statDate, @Param("coveredUntil") String coveredUntil,
            @Param("availableFrom") String availableFrom, @Param("statisticsCompletedAt") String statisticsCompletedAt);

    List<MonitorCoverageRow> queryCoverage(MonitorRange range);

    void saveMetrics(@Param("statDate") String statDate, @Param("coveredUntil") String coveredUntil);

    void saveCoverage(@Param("statDate") String statDate, @Param("coveredUntil") String coveredUntil,
            @Param("statisticsCompletedAt") String statisticsCompletedAt);
}
