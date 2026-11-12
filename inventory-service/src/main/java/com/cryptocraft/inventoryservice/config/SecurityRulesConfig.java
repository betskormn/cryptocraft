package com.cryptocraft.inventoryservice.config;

import com.cryptocraft.security.ServiceSecurityRules;
import com.cryptocraft.security.VenueRole;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

import static com.cryptocraft.security.VenueAccess.platformAdmin;
import static com.cryptocraft.security.VenueAccess.platformAdminOrService;
import static com.cryptocraft.security.VenueAccess.venueRole;

/**
 * Who may call what (security-support checks the token and the venue in X-Tenant-Id):
 * the menu and stock belong to admins and inventory managers; floor staff may read items and use the
 * stop-list from the kitchen/bar screens; the platform API is for platform admins.
 */
@Configuration
public class SecurityRulesConfig {

    @Bean
    public ServiceSecurityRules inventorySecurityRules() {
        return rules -> rules
                // the guests' menu; also checked by Order Service when an order is placed
                .requestMatchers(HttpMethod.GET, "/api/v1/items/sellable").permitAll()
                // recipe write-offs come from Order Service only
                .requestMatchers(HttpMethod.POST, "/api/v1/stock-consumptions").access(platformAdminOrService())
                .requestMatchers(HttpMethod.GET, "/api/v1/stock-consumptions/**").access(venueRole(VenueRole.ADMIN, VenueRole.INVENTORY_MANAGER))
                .requestMatchers(HttpMethod.PATCH, "/api/v1/items/*/stop-list").access(venueRole())
                .requestMatchers(HttpMethod.GET, "/api/v1/items", "/api/v1/items/*").access(venueRole())
                .requestMatchers("/api/v1/brands/**", "/api/v1/products/**", "/api/v1/items/**")
                .access(venueRole(VenueRole.ADMIN, VenueRole.INVENTORY_MANAGER))
                // Order Service checks that a venue exists and is active
                .requestMatchers(HttpMethod.GET, "/api/v1/platform/tenants/*").access(platformAdminOrService())
                .requestMatchers("/api/v1/platform/**").access(platformAdmin())
                .requestMatchers(HttpMethod.GET, "/api/v1/venues").authenticated();
    }
}
