package com.cmbchina.infrastructure.monitor.dao.config;

import com.cmbchina.infrastructure.monitor.dao.mapper.MonitorStatisticsMapper;
import java.io.IOException;
import java.io.InputStream;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/** 为监控查询注册独立的 ClickHouse Mapper，保留 MySQL 的自动配置。 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "app.monitor.mock-enabled", havingValue = "false", matchIfMissing = true)
public class MonitorClickHouseConfiguration {
    @Bean
    public MonitorStatisticsMapper monitorStatisticsMapper(@Value("${app.monitor.clickhouse-jdbc-url:}") String url,
            @Value("${app.monitor.clickhouse-user:default}") String user, @Value("${app.monitor.clickhouse-password:}") String password)
            throws IOException {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(url, user, password);
        dataSource.setDriverClassName("com.clickhouse.jdbc.ClickHouseDriver");

        Environment environment = new Environment("monitorClickHouse", new SpringManagedTransactionFactory(), dataSource);
        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration(environment);
        configuration.setMapUnderscoreToCamelCase(true);
        ClassPathResource mapper = new ClassPathResource("clickhouse/MonitorStatisticsMapper.xml");
        try (InputStream input = mapper.getInputStream()) {
            new XMLMapperBuilder(input, configuration, mapper.getPath(), configuration.getSqlFragments()).parse();
        }
        SqlSessionTemplate session = new SqlSessionTemplate(new SqlSessionFactoryBuilder().build(configuration));
        return session.getMapper(MonitorStatisticsMapper.class);
    }
}
