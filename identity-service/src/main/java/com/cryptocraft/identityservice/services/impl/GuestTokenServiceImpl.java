package com.cryptocraft.identityservice.services.impl;

import com.cryptocraft.identityservice.config.IdentityProperties;
import com.cryptocraft.identityservice.controllers.dto.request.GuestTokenRequestDto;
import com.cryptocraft.identityservice.controllers.dto.response.GuestTokenResponseDto;
import com.cryptocraft.identityservice.services.GuestTokenService;
import com.cryptocraft.security.CryptoCraftClaims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Guests have no account: their token names the venue and the guest of one visit, is signed with the same
 * key as staff tokens and is checked the same way by every service. Order Service decides who may join;
 * this only signs. The visit itself is checked by Order on every request, so closing it ends the token's use.
 */
@Service
@RequiredArgsConstructor
public class GuestTokenServiceImpl implements GuestTokenService {

    public static final String GUEST_AUDIENCE = "cryptocraft-guest";

    private final JwtEncoder jwtEncoder;
    private final IdentityProperties properties;

    @Override
    public GuestTokenResponseDto issue(GuestTokenRequestDto requestDto) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(properties.guestTokenTtl());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject("guest:" + requestDto.participantId())
                .audience(List.of(GUEST_AUDIENCE))
                .issuedAt(now)
                .expiresAt(expiresAt)
                .id(UUID.randomUUID().toString())
                .claim(CryptoCraftClaims.GUEST, true)
                .claim(CryptoCraftClaims.TENANT_ID, requestDto.tenantId())
                .claim(CryptoCraftClaims.PARTICIPANT_ID, requestDto.participantId().toString())
                .claim(CryptoCraftClaims.TABLE_SESSION_ID, requestDto.sessionId().toString())
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).build(), claims))
                .getTokenValue();
        return new GuestTokenResponseDto(token, expiresAt);
    }
}
