package com.cryptocraft.inventoryservice.config;

import com.cryptocraft.inventoryservice.web.TenantFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebFiltersConfig {

    @Bean
    public FilterRegistrationBean<TenantFilter> tenantFilter() {
        FilterRegistrationBean<TenantFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new TenantFilter());
        bean.setOrder(0); // виконати якнайраніше
        return bean;
    }
}