package com.cmbchina.infrastructure.monitor.supportimpl;

import com.cmbchina.domain.monitor.model.MonitorCoverage;
import com.cmbchina.domain.monitor.model.MonitorFilter;
import com.cmbchina.domain.monitor.model.MonitorMetric;
import com.cmbchina.domain.monitor.model.MonitorMetricRow;
import com.cmbchina.domain.monitor.model.MonitorRange;
import com.cmbchina.domain.monitor.model.MonitorRefreshCommand;
import com.cmbchina.domain.monitor.support.MonitorDataSupport;
import com.cmbchina.infrastructure.common.ResponseEnum;
import com.cmbchina.infrastructure.common.exception.BusinessException;
import com.cmbchina.infrastructure.monitor.dao.mapper.MonitorNameMapper;
import com.cmbchina.infrastructure.monitor.dao.mapper.MonitorNameRecord;
import com.cmbchina.infrastructure.monitor.dao.mapper.MonitorStatisticsMapper;
import com.cmbchina.infrastructure.monitor.dao.po.MonitorCoverageRow;
import com.cmbchina.infrastructure.monitor.dao.po.MonitorStatisticsRow;
import org.springframework.dao.DataAccessException;
import com.google.common.collect.Maps;
import java.util.stream.Collectors;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** 读取 ClickHouse 离线统计，并从合并后的业务库补齐名称。 */
@Component
@ConditionalOnProperty(name = "app.monitor.mock-enabled", havingValue = "false", matchIfMissing = true)
public class MonitorDataSupportImpl implements MonitorDataSupport {
    private static final int BUCKET_MINUTES = 10;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    @Autowired
    private MonitorNameMapper nameMapper;

    @Autowired
    private MonitorStatisticsMapper statisticsMapper;

    /** 查询平铺统计结果。 */
    @Override
    public List<MonitorMetricRow> queryStatistics(MonitorFilter request) {
        try {
            return statisticsMapper.queryStatistics(request).stream().map(this::metricRow).collect(Collectors.toList());
        } catch (DataAccessException error) {
            throw new BusinessException(ResponseEnum.MONITOR_UPSTREAM_ERROR.getCode(), "ClickHouse 监控统计查询失败", error);
        }
    }

    /** 查询离线覆盖结果。 */
    @Override
    public List<MonitorCoverage> queryCoverage(MonitorRange request) {
        try {
            return statisticsMapper.queryCoverage(request).stream().map(this::coverageRow).collect(Collectors.toList());
        } catch (DataAccessException error) {
            throw new BusinessException(ResponseEnum.MONITOR_UPSTREAM_ERROR.getCode(), "ClickHouse 监控覆盖查询失败", error);
        }
    }

    private MonitorCoverage coverageRow(MonitorCoverageRow row) {
        return MonitorCoverage.builder().availableFrom(toApiTime(row.getAvailableFrom())).availableThrough(toApiTime(row.getAvailableThrough()))
                .effectiveSeconds(row.getEffectiveSeconds()).statisticsCompletedAt(toApiTime(row.getStatisticsCompletedAt())).build();
    }

    /** 按闭合的十分钟桶逐日重算并覆盖写入离线指标。 */
    @Override
    public synchronized void refresh(MonitorRefreshCommand request) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        long offset = request.getOffset();
        LocalDate start;
        LocalDate end;
        if ("week".equals(request.getPeriodType())) {
            start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(offset);
            end = start.plusWeeks(1);
        } else if ("month".equals(request.getPeriodType())) {
            start = today.withDayOfMonth(1).minusMonths(offset);
            end = start.plusMonths(1);
        } else {
            start = today.minusDays(offset);
            end = start.plusDays(1);
        }
        if (start.toEpochDay() < 0) {
            throw new BusinessException(ResponseEnum.VALIDATION_ERROR.getCode(), "离线更新日期超出 ClickHouse 支持范围", null);
        }
        LocalDateTime cutoff = now.truncatedTo(ChronoUnit.MINUTES).minusMinutes(now.getMinute() % BUCKET_MINUTES);
        try {
            for (LocalDate date = start; date.isBefore(end) && date.atStartOfDay().isBefore(cutoff); date = date.plusDays(1)) {
                LocalDateTime nextDay = date.plusDays(1).atStartOfDay();
                String statDate = date.toString();
                String coveredUntil = TIME.format(nextDay.isBefore(cutoff) ? nextDay : cutoff);

                statisticsMapper.saveMetrics(statDate, coveredUntil);
                statisticsMapper.saveCoverage(statDate, coveredUntil, TIME.format(LocalDateTime.now()));

            }
        } catch (DataAccessException error) {
            throw new BusinessException(ResponseEnum.MONITOR_UPSTREAM_ERROR.getCode(), "ClickHouse 监控离线更新失败", error);
        }
    }

    /** 将 ClickHouse 协议值转换为领域指标。 */
    private MonitorMetricRow metricRow(MonitorStatisticsRow row) {
        MonitorMetric metric = MonitorMetric.builder().startedCount(row.getStartedCount()).successCount(row.getSuccessCount())
                .failureCount(row.getFailureCount()).timeoutCount(row.getTimeoutCount()).cancelledCount(row.getCancelledCount())
                .cacheReadCount(row.getCacheReadCount()).cacheHitCount(row.getCacheHitCount()).expertCount(row.getExpertCount())
                .interfaceCount(row.getInterfaceCount()).latencySampleCount(row.getLatencySampleCount()).averageQps(row.getAverageQps())
                .successTps(row.getSuccessTps()).errorRate(row.getErrorRate()).timeoutRate(row.getTimeoutRate()).cacheHitRate(row.getCacheHitRate())
                .p95Millis(row.getP95Millis()).p99Millis(row.getP99Millis()).build();
        return MonitorMetricRow.builder().groupKey(row.getGroupKey()).metricScope(row.getMetricScope()).bucketStart(toApiTime(row.getBucketStart()))
                .bucketEnd(toApiTime(row.getBucketEnd())).effectiveSeconds(row.getEffectiveSeconds()).metric(metric).build();
    }

    /** 将 ClickHouse 返回的日期时间统一为三位毫秒。 */
    private String toApiTime(String value) {
        return value == null ? null : TIME.format(LocalDateTime.parse(value.replace(' ', 'T')));
    }

    /** 从当前数据源批量读取名称。 */
    @Override
    public Map<String, String> names(String group, Set<String> ids) {
        List<MonitorNameRecord> records = "interface".equals(group) ? nameMapper.findInterfaces(ids) : nameMapper.findExperts(ids);
        Map<String, String> names = Maps.newHashMap();
        for (MonitorNameRecord record : records) {
            names.put(record.getId(), record.getName());
        }
        return names;
    }
}
