package com.cmbchina.infrastructure.monitor.dao.mapper;

import java.util.List;
import java.util.Set;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 查询合并库中的接口与专家名称。 */
@Mapper
public interface MonitorNameMapper {

    /** 批量查询接口标识和名称。 */
    List<MonitorNameRecord> findInterfaces(@Param("ids") Set<String> ids);

    /** 批量查询专家标识和名称。 */
    List<MonitorNameRecord> findExperts(@Param("ids") Set<String> ids);
}
