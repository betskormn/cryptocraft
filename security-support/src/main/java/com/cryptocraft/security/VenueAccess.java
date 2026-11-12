package com.cryptocraft.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Authorization rules for {@link ServiceSecurityRules}. The venue of a request is its X-Tenant-Id header;
 * a staff token grants access only to the venues listed in it.
 */
public final class VenueAccess {

    public static final String TENANT_HEADER = "X-Tenant-Id";

    private VenueAccess() {
    }

    /**
     * A member of the request's venue with one of the roles, or a platform admin.
     */
    public static AuthorizationManager<RequestAuthorizationContext> venueRole(VenueRole... roles) {
        Set<VenueRole> allowed = roles.length == 0 ? EnumSet.allOf(VenueRole.class) : EnumSet.copyOf(Arrays.asList(roles));
        return (authentication, context) -> new AuthorizationDecision(
                jwt(authentication.get())
                        .map(jwt -> isPlatformAdmin(jwt) || roleIn(jwt, context.getRequest()).map(allowed::contains).orElse(false))
                        .orElse(false));
    }

    /**
     * A member of the request's venue with one of the roles, a platform admin, or one of our services.
     */
    public static AuthorizationManager<RequestAuthorizationContext> venueRoleOrService(VenueRole... roles) {
        AuthorizationManager<RequestAuthorizationContext> member = venueRole(roles);
        return (authentication, context) -> isService(authentication.get())
                ? new AuthorizationDecision(true)
                : member.authorize(authentication, context);
    }

    /**
     * A guest of a visit in the request's venue (guest token from joining a table).
     */
    public static AuthorizationManager<RequestAuthorizationContext> guest() {
        return (authentication, context) -> new AuthorizationDecision(
                jwt(authentication.get())
                        .filter(VenueAccess::isGuest)
                        .map(jwt -> tenantOf(context.getRequest()).map(id -> id.equals(guestTenant(jwt))).orElse(false))
                        .orElse(false));
    }

    /**
     * One of our services (client credentials, scope "internal").
     */
    public static AuthorizationManager<RequestAuthorizationContext> service() {
        return (authentication, context) -> new AuthorizationDecision(isService(authentication.get()));
    }

    public static boolean isGuest(Jwt jwt) {
        return Boolean.TRUE.equals(jwt.getClaimAsBoolean(CryptoCraftClaims.GUEST));
    }

    /**
     * The guest a guest token was issued to; empty for any other token.
     */
    public static Optional<UUID> guestParticipant(Jwt jwt) {
        if (!isGuest(jwt)) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(jwt.getClaimAsString(CryptoCraftClaims.PARTICIPANT_ID)));
        } catch (IllegalArgumentException | NullPointerException e) {
            return Optional.empty();
        }
    }

    private static Long guestTenant(Jwt jwt) {
        Object claim = jwt.getClaim(CryptoCraftClaims.TENANT_ID);
        try {
            return claim == null ? null : Long.parseLong(String.valueOf(claim));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static AuthorizationManager<RequestAuthorizationContext> platformAdmin() {
        return (authentication, context) -> new AuthorizationDecision(jwt(authentication.get()).map(VenueAccess::isPlatformAdmin).orElse(false));
    }

    public static AuthorizationManager<RequestAuthorizationContext> platformAdminOrService() {
        return (authentication, context) -> new AuthorizationDecision(
                isService(authentication.get()) || jwt(authentication.get()).map(VenueAccess::isPlatformAdmin).orElse(false));
    }

    /**
     * Venues a token gives access to; empty for a platform admin, who may open any venue.
     */
    public static Map<Long, VenueRole> venues(Jwt jwt) {
        Object claim = jwt.getClaim(CryptoCraftClaims.VENUES);
        if (!(claim instanceof Map<?, ?> raw)) {
            return Map.of();
        }
        HashMap<Long, VenueRole> venues = new HashMap<>();
        raw.forEach((tenantId, role) -> {
            try {
                venues.put(Long.parseLong(String.valueOf(tenantId)), VenueRole.valueOf(String.valueOf(role)));
            } catch (IllegalArgumentException ignored) {
                // a role or id this service does not know grants nothing
            }
        });
        return Map.copyOf(venues);
    }

    public static boolean isPlatformAdmin(Jwt jwt) {
        return Boolean.TRUE.equals(jwt.getClaimAsBoolean(CryptoCraftClaims.PLATFORM_ADMIN));
    }

    private static boolean isService(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated() && authentication.getAuthorities().stream()
                .anyMatch(a -> ("SCOPE_" + CryptoCraftClaims.SCOPE_INTERNAL).equals(a.getAuthority()));
    }

    private static Optional<Jwt> jwt(Authentication authentication) {
        return authentication instanceof JwtAuthenticationToken token && token.isAuthenticated()
                ? Optional.of(token.getToken())
                : Optional.empty();
    }

    private static Optional<VenueRole> roleIn(Jwt jwt, HttpServletRequest request) {
        return tenantOf(request).map(tenantId -> venues(jwt).get(tenantId));
    }

    private static Optional<Long> tenantOf(HttpServletRequest request) {
        String header = request.getHeader(TENANT_HEADER);
        if (header == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(header.trim()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
