package com.cryptocraft.inventoryservice.config;

import com.cryptocraft.inventoryservice.config.TenantAwareDataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Bean
    @ConditionalOnBean(DataSource.class)
    public DataSource tenantAwareDataSource(DataSource target) {
        // Boot створює HikariDataSource (або інший) як DataSource-бін.
        // Ми повертаємо обгорнутий DataSource, який застосовує SET LOCAL щоразу при getConnection().
        return new TenantAwareDataSource(target);
    }
}