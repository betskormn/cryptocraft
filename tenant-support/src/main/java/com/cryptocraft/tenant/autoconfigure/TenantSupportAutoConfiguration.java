package com.cryptocraft.tenant.autoconfigure;

import com.cryptocraft.tenant.TenantStatusLookup;
import com.cryptocraft.tenant.jdbc.TenantAwareDataSource;
import com.cryptocraft.tenant.web.TenantFilter;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

import javax.sql.DataSource;

/**
 * Every DataSource is wrapped so connections carry the request's tenant (for PostgreSQL RLS), and
 * requests are scoped to a venue by X-Tenant-Id once the service provides a {@link TenantStatusLookup}.
 */
@AutoConfiguration
@EnableConfigurationProperties(TenantProperties.class)
public class TenantSupportAutoConfiguration {

    @Bean
    @ConditionalOnClass(DataSource.class)
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

    @Bean
    @ConditionalOnBean(TenantStatusLookup.class)
    public FilterRegistrationBean<TenantFilter> tenantFilter(TenantStatusLookup tenantStatusLookup,
                                                             TenantProperties properties) {
        FilterRegistrationBean<TenantFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new TenantFilter(tenantStatusLookup, properties.freePaths()));
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE + 10); // before anything that reads tenant data
        return bean;
    }
}
