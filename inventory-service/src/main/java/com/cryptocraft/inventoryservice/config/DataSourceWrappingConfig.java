package com.cryptocraft.inventoryservice.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class DataSourceWrappingConfig {

    @Bean
    public static BeanPostProcessor tenantAwareDataSourcePostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof DataSource ds && !(ds instanceof TenantAwareDataSource)) {
                    return new TenantAwareDataSource(ds);
                }
                return bean;
            }
        };
    }
}