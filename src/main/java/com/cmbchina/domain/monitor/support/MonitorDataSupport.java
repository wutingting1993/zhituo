package com.cmbchina.domain.monitor.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.util.Map;
import java.util.Set;

/** 监控统计和名称数据来源。 */
public interface MonitorDataSupport {

    /** 获取已发布的统计记录。 */
    ArrayNode queryStatistics(JsonNode request);

    /** 获取离线统计覆盖记录。 */
    ArrayNode queryCoverage(JsonNode request);

    /** 调用离线统计更新接口。 */
    JsonNode refresh(JsonNode request);

    /** 查询当前维度的名称。 */
    Map<String, String> names(String group, Set<String> ids);
}
