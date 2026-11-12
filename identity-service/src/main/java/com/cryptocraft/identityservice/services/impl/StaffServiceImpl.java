package com.cryptocraft.identityservice.services.impl;

import com.cryptocraft.identityservice.controllers.dto.request.StaffRequestDto;
import com.cryptocraft.identityservice.controllers.dto.request.StaffRoleRequestDto;
import com.cryptocraft.identityservice.controllers.dto.response.StaffResponseDto;
import com.cryptocraft.identityservice.dao.StaffUser;
import com.cryptocraft.identityservice.dao.VenueMembership;
import com.cryptocraft.identityservice.exceptions.StaffConflictException;
import com.cryptocraft.identityservice.repositories.StaffUserRepository;
import com.cryptocraft.identityservice.repositories.VenueMembershipRepository;
import com.cryptocraft.identityservice.services.StaffService;
import com.cryptocraft.security.VenueRole;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * The people of one venue. A venue always keeps at least one admin, so it cannot lock itself out.
 */
@Service
@RequiredArgsConstructor
public class StaffServiceImpl implements StaffService {

    private final StaffUserRepository staffUserRepository;
    private final VenueMembershipRepository venueMembershipRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public List<StaffResponseDto> getStaff(Long tenantId) {
        return venueMembershipRepository.findStaffOfVenue(tenantId).stream()
                .map(StaffServiceImpl::toDto)
                .toList();
    }

    @Override
    @Transactional
    public StaffResponseDto addStaff(Long tenantId, StaffRequestDto requestDto) {
        String email = requestDto.email().trim();
        StaffUser user = staffUserRepository.findByEmailIgnoreCase(email)
                .orElseGet(() -> createAccount(email, requestDto));
        if (venueMembershipRepository.existsById(new VenueMembership.Id(user.getUserId(), tenantId))) {
            throw new StaffConflictException("%s already works in this venue".formatted(email));
        }
        VenueMembership membership = venueMembershipRepository.save(VenueMembership.builder()
                .id(new VenueMembership.Id(user.getUserId(), tenantId))
                .user(user)
                .role(requestDto.role())
                .build());
        return toDto(membership);
    }

    @Override
    @Transactional
    public StaffResponseDto changeRole(Long tenantId, UUID userId, StaffRoleRequestDto requestDto) {
        VenueMembership membership = findMembership(tenantId, userId);
        if (membership.getRole() == VenueRole.ADMIN && requestDto.role() != VenueRole.ADMIN) {
            requireAnotherAdmin(tenantId);
        }
        membership.setRole(requestDto.role());
        return toDto(membership);
    }

    @Override
    @Transactional
    public void removeStaff(Long tenantId, UUID userId) {
        VenueMembership membership = findMembership(tenantId, userId);
        if (membership.getRole() == VenueRole.ADMIN) {
            requireAnotherAdmin(tenantId);
        }
        // the account stays: the person may work in other venues
        membership.getUser().getMemberships().remove(membership);
        venueMembershipRepository.delete(membership);
    }

    private StaffUser createAccount(String email, StaffRequestDto requestDto) {
        if (requestDto.password() == null) {
            throw new IllegalArgumentException("A new account needs an initial password");
        }
        if (requestDto.displayName() == null || requestDto.displayName().isBlank()) {
            throw new IllegalArgumentException("A new account needs a name");
        }
        return staffUserRepository.save(StaffUser.builder()
                .userId(UUID.randomUUID())
                .email(email)
                .displayName(requestDto.displayName().trim())
                .passwordHash(passwordEncoder.encode(requestDto.password()))
                .enabled(true)
                .platformAdmin(false)
                .build());
    }

    private VenueMembership findMembership(Long tenantId, UUID userId) {
        return venueMembershipRepository.findById(new VenueMembership.Id(userId, tenantId))
                .orElseThrow(() -> new EntityNotFoundException("This person does not work in this venue"));
    }

    private void requireAnotherAdmin(Long tenantId) {
        if (venueMembershipRepository.countByIdTenantIdAndRole(tenantId, VenueRole.ADMIN) <= 1) {
            throw new StaffConflictException("The venue must keep at least one admin");
        }
    }

    private static StaffResponseDto toDto(VenueMembership membership) {
        StaffUser user = membership.getUser();
        return new StaffResponseDto(user.getUserId(), user.getEmail(), user.getDisplayName(), membership.getRole(), user.isEnabled());
    }
}
