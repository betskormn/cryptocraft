package com.cryptocraft.security.test;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Access tokens for integration tests, signed with a key made for the test run. The service under test
 * trusts it through {@code cryptocraft.security.public-key=TestTokens.publicKey()}.
 */
public final class TestTokens {

    private static final KeyPair KEYS = generate();
    private static final NimbusJwtEncoder ENCODER = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(
            new RSAKey.Builder((RSAPublicKey) KEYS.getPublic()).privateKey((RSAPrivateKey) KEYS.getPrivate()).build())));

    private TestTokens() {
    }

    public static String publicKey() {
        return Base64.getEncoder().encodeToString(KEYS.getPublic().getEncoded());
    }

    // a staff member of one venue; role is a VenueRole name
    public static String venue(long tenantId, String role) {
        return sign(Map.of("venues", Map.of(String.valueOf(tenantId), role)));
    }

    public static String platformAdmin() {
        return sign(Map.of("platform_admin", true));
    }

    // a guest of a visit, as identity-service issues it when the guest joins a table
    public static String guest(long tenantId, UUID participantId) {
        return sign(Map.of("guest", true, "tenant_id", tenantId, "participant_id", participantId.toString()));
    }

    // a client_credentials token of one of our services
    public static String service() {
        return sign(Map.of("scope", List.of("internal")));
    }

    public static String sign(Map<String, Object> claims) {
        Instant now = Instant.now();
        JwtClaimsSet.Builder builder = JwtClaimsSet.builder()
                .issuer("http://identity.test")
                .subject("test-user")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(600));
        claims.forEach(builder::claim);
        return ENCODER.encode(JwtEncoderParameters.from(builder.build())).getTokenValue();
    }

    private static KeyPair generate() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
