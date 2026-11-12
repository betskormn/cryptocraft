package com.cryptocraft.orderservice.services.impl;

import com.cryptocraft.orderservice.clients.IdentityClient;
import com.cryptocraft.orderservice.clients.InventoryClient;
import com.cryptocraft.orderservice.clients.dto.GuestTokenDto;
import com.cryptocraft.orderservice.clients.dto.TenantDto;
import com.cryptocraft.orderservice.controllers.dto.request.JoinTableRequestDto;
import com.cryptocraft.orderservice.controllers.dto.response.JoinTableResponseDto;
import com.cryptocraft.orderservice.exceptions.DownstreamUnavailableException;
import com.cryptocraft.orderservice.services.GuestJoinService;
import com.cryptocraft.orderservice.services.TableSessionService;
import com.cryptocraft.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * The visit is joined in one short transaction; the token and the venue's details are fetched after it,
 * so no database connection waits for identity-service or Inventory.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GuestJoinServiceImpl implements GuestJoinService {

    private final TableSessionService tableSessionService;
    private final IdentityClient identityClient;
    private final InventoryClient inventoryClient;

    @Override
    public JoinTableResponseDto join(JoinTableRequestDto requestDto, UUID currentParticipantId) {
        Long tenantId = TenantContext.getRequired();
        TableSessionService.JoinedGuest guest = tableSessionService.join(requestDto, currentParticipantId);
        GuestTokenDto token = identityClient.issueGuestToken(tenantId, guest.participantId(), guest.sessionId());
        Optional<TenantDto> venue = venue(tenantId);
        return new JoinTableResponseDto(
                guest.sessionId(),
                guest.participantId(),
                guest.tableId(),
                guest.tableLabel(),
                guest.status(),
                token.token(),
                token.expiresAt(),
                venue.map(TenantDto::name).orElse(null),
                venue.map(TenantDto::slug).orElse(null),
                venue.map(TenantDto::currency).orElse(null));
    }

    // only for the guest's page; joining does not depend on it
    private Optional<TenantDto> venue(Long tenantId) {
        try {
            return inventoryClient.getTenant(tenantId);
        } catch (DownstreamUnavailableException e) {
            log.warn("Venue {} details not available for a joining guest: {}", tenantId, e.getMessage());
            return Optional.empty();
        }
    }
}
