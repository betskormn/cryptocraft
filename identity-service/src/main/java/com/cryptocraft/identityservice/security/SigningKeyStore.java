package com.cryptocraft.identityservice.security;

import com.nimbusds.jose.jwk.RSAKey;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * The RSA key that signs tokens. Made once and kept in the database, so tokens survive a restart and every
 * instance signs with the same key; services fetch the public part from /oauth2/jwks.
 */
@Component
@RequiredArgsConstructor
public class SigningKeyStore {

    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public RSAKey currentKey() {
        // one key at a time for now; rotation would add a new row and publish both until old tokens expire
        jdbcTemplate.execute("LOCK TABLE signing_keys IN SHARE ROW EXCLUSIVE MODE");
        List<RSAKey> keys = jdbcTemplate.query(
                "SELECT key_id, public_key, private_key FROM signing_keys ORDER BY created_at DESC LIMIT 1",
                (rs, i) -> toKey(rs.getString("key_id"), rs.getString("public_key"), rs.getString("private_key")));
        if (!keys.isEmpty()) {
            return keys.getFirst();
        }
        KeyPair pair = generate();
        String keyId = UUID.randomUUID().toString();
        Base64.Encoder base64 = Base64.getEncoder();
        jdbcTemplate.update("INSERT INTO signing_keys (key_id, public_key, private_key) VALUES (?, ?, ?)",
                keyId, base64.encodeToString(pair.getPublic().getEncoded()), base64.encodeToString(pair.getPrivate().getEncoded()));
        return new RSAKey.Builder((RSAPublicKey) pair.getPublic()).privateKey((RSAPrivateKey) pair.getPrivate()).keyID(keyId).build();
    }

    private static RSAKey toKey(String keyId, String publicKey, String privateKey) {
        try {
            KeyFactory factory = KeyFactory.getInstance("RSA");
            Base64.Decoder base64 = Base64.getDecoder();
            RSAPublicKey pub = (RSAPublicKey) factory.generatePublic(new X509EncodedKeySpec(base64.decode(publicKey)));
            RSAPrivateKey priv = (RSAPrivateKey) factory.generatePrivate(new PKCS8EncodedKeySpec(base64.decode(privateKey)));
            return new RSAKey.Builder(pub).privateKey(priv).keyID(keyId).build();
        } catch (Exception e) {
            throw new IllegalStateException("Signing key " + keyId + " cannot be read", e);
        }
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
