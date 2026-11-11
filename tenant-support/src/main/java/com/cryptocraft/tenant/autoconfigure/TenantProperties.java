package com.cryptocraft.tenant.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * @param freePaths path prefixes served without X-Tenant-Id (service-to-service and tooling endpoints)
 */
@ConfigurationProperties("tenant")
public record TenantProperties(List<String> freePaths) {

    public TenantProperties {
        if (freePaths == null) {
            freePaths = List.of("/internal/", "/actuator", "/v3/api-docs", "/swagger-ui", "/error");
        }
    }
}
