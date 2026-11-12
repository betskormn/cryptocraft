package com.cryptocraft.orderservice.config;

import com.cryptocraft.security.ServiceSecurityRules;
import com.cryptocraft.security.VenueRole;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

import static com.cryptocraft.security.VenueAccess.guest;
import static com.cryptocraft.security.VenueAccess.venueRole;

/**
 * Who may call what (security-support checks the token and the venue in X-Tenant-Id).
 * Guests have no accounts: joining a table by QR code is open and gives a guest token (signed by
 * identity-service) for ordering and the guest's own bill in that venue.
 */
@Configuration
public class SecurityRulesConfig {

    @Bean
    public ServiceSecurityRules orderSecurityRules() {
        return rules -> rules
                .requestMatchers(HttpMethod.POST, "/api/v1/table-sessions/join").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/orders").access(guest())
                .requestMatchers(HttpMethod.GET, "/api/v1/participants/me/bill").access(guest())
                // tables and their QR codes belong to the venue admin
                .requestMatchers("/api/v1/tables/**").access(venueRole(VenueRole.ADMIN))
                // floor, kitchen and bar
                .requestMatchers("/api/v1/table-sessions/**", "/api/v1/orders/**", "/api/v1/kitchen/**")
                .access(venueRole(VenueRole.ADMIN, VenueRole.STAFF));
    }
}
