package com.cryptocraft.identityservice.security;

import com.cryptocraft.identityservice.dao.StaffUser;
import com.cryptocraft.identityservice.repositories.StaffUserRepository;
import com.cryptocraft.security.CryptoCraftClaims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Puts the person's venues and roles into their tokens. They are read from the database every time a token
 * is issued, so a role change or a removal reaches an open session at its next refresh (minutes), and a
 * disabled account cannot refresh at all.
 */
@Component
@RequiredArgsConstructor
public class TokenClaimsCustomizer implements OAuth2TokenCustomizer<JwtEncodingContext> {

    private final StaffUserRepository staffUserRepository;

    @Override
    @Transactional(readOnly = true)
    public void customize(JwtEncodingContext context) {
        // service tokens (client credentials) have no person behind them
        if (AuthorizationGrantType.CLIENT_CREDENTIALS.equals(context.getAuthorizationGrantType())) {
            return;
        }
        boolean accessToken = OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType());
        boolean idToken = OidcParameterNames.ID_TOKEN.equals(context.getTokenType().getValue());
        if (!accessToken && !idToken) {
            return;
        }
        StaffUser user = findUser(context.getPrincipal().getName());

        context.getClaims().claim(CryptoCraftClaims.NAME, user.getDisplayName());
        context.getClaims().claim(CryptoCraftClaims.EMAIL, user.getEmail());
        if (accessToken) {
            Map<String, String> venues = new LinkedHashMap<>();
            user.getMemberships().forEach(m -> venues.put(String.valueOf(m.getTenantId()), m.getRole().name()));
            context.getClaims().claim(CryptoCraftClaims.VENUES, venues);
            context.getClaims().claim(CryptoCraftClaims.PLATFORM_ADMIN, user.isPlatformAdmin());
        }
    }

    private StaffUser findUser(String userId) {
        try {
            return staffUserRepository.findWithMembershipsByUserId(UUID.fromString(userId))
                    .filter(StaffUser::isEnabled)
                    .orElseThrow(TokenClaimsCustomizer::accountGone);
        } catch (IllegalArgumentException e) {
            throw accountGone();
        }
    }

    private static OAuth2AuthenticationException accountGone() {
        return new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.INVALID_GRANT, "The account is disabled or removed", null));
    }
}
