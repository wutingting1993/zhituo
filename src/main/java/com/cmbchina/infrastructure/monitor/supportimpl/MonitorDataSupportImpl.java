package com.cmbchina.infrastructure.monitor.supportimpl;

import com.cmbchina.domain.monitor.support.MonitorDataSupport;
import com.cmbchina.infrastructure.common.ResponseEnum;
import com.cmbchina.infrastructure.common.exception.BusinessException;
import com.cmbchina.infrastructure.monitor.dao.mapper.MonitorNameMapper;
import com.cmbchina.infrastructure.monitor.dao.mapper.MonitorNameRecord;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.Maps;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/** 读取 ClickHouse 离线统计，并从合并后的业务库补齐名称。 */
@Component
@ConditionalOnProperty(name = "app.monitor.mock-enabled", havingValue = "false", matchIfMissing = true)
public class MonitorDataSupportImpl implements MonitorDataSupport {

    @Value("${app.monitor.clickhouse-base-url:}")
    private String baseUrl;
    @Autowired
    private RestTemplateBuilder builder;
    @Autowired
    private MonitorNameMapper nameMapper;

    private RestTemplate queryClient;
    private RestTemplate refreshClient;

    /** 分别初始化查询和手动重算的传输时限。 */
    @PostConstruct
    public void initialize() {
        RestTemplateBuilder base = builder.setConnectTimeout(Duration.ofSeconds(5));
        queryClient = base.setReadTimeout(Duration.ofSeconds(20)).build();
        refreshClient = base.setReadTimeout(Duration.ofMinutes(2)).build();
    }

    /** 查询指定的平铺 CK 统计结果。 */
    @Override
    public ArrayNode query(String path, JsonNode request) {
        JsonNode data = invoke(queryClient, path, request);
        if (!data.isArray()) {
            throw new BusinessException(ResponseEnum.MONITOR_UPSTREAM_ERROR.getCode(), "监控统计响应格式不正确", null);
        }
        return (ArrayNode) data;
    }

    /** 转发离线更新。 */
    @Override
    public JsonNode refresh(JsonNode request) {
        return invoke(refreshClient, "interface-monitor-offline-refresh", request);
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

    /** 统一解包 CK 响应，并保留传输异常。 */
    private JsonNode invoke(RestTemplate client, String path, JsonNode request) {
        if (baseUrl.trim().isEmpty()) {
            throw new BusinessException(ResponseEnum.MONITOR_UPSTREAM_ERROR.getCode(), "监控数据服务尚未配置", null);
        }
        String url = baseUrl.endsWith("/") ? baseUrl + path : baseUrl + "/" + path;
        JsonNode response;
        try {
            response = client.postForObject(url, request, JsonNode.class);
        } catch (RestClientException error) {
            throw new BusinessException(ResponseEnum.MONITOR_UPSTREAM_ERROR.getCode(), "监控数据服务暂不可用", error);
        }
        if (response == null || !response.path("success").asBoolean() || !response.has("data")) {
            throw new BusinessException(ResponseEnum.MONITOR_UPSTREAM_ERROR.getCode(), "监控数据服务处理失败", null);
        }
        return response.get("data");
    }
}
