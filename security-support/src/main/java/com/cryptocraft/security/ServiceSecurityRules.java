package com.cryptocraft.security;

import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

/**
 * Who may call which endpoint of a service. Common paths (health, API docs, /internal/**) are already
 * open; anything a service does not list requires a valid token.
 *
 * <pre>
 * rules.requestMatchers(HttpMethod.GET, "/api/v1/items/sellable").permitAll()
 *      .requestMatchers("/api/v1/items/**").access(VenueAccess.venueRole(VenueRole.ADMIN, VenueRole.INVENTORY_MANAGER));
 * </pre>
 */
@FunctionalInterface
public interface ServiceSecurityRules {

    void configure(AuthorizeHttpRequestsConfigurer<?>.AuthorizationManagerRequestMatcherRegistry rules);
}
