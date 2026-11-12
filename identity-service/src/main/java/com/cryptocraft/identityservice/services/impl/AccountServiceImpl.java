package com.cryptocraft.identityservice.services.impl;

import com.cryptocraft.identityservice.controllers.dto.request.PasswordChangeRequestDto;
import com.cryptocraft.identityservice.controllers.dto.response.MeResponseDto;
import com.cryptocraft.identityservice.controllers.dto.response.VenueAccessDto;
import com.cryptocraft.identityservice.dao.StaffUser;
import com.cryptocraft.identityservice.exceptions.StaffConflictException;
import com.cryptocraft.identityservice.repositories.StaffUserRepository;
import com.cryptocraft.identityservice.services.AccountService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final StaffUserRepository staffUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public MeResponseDto getMe(UUID userId) {
        StaffUser user = staffUserRepository.findWithMembershipsByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Account not found"));
        return new MeResponseDto(
                user.getUserId(),
                user.getEmail(),
                user.getDisplayName(),
                user.isPlatformAdmin(),
                user.getMemberships().stream()
                        .map(m -> new VenueAccessDto(m.getTenantId(), m.getRole()))
                        .sorted(Comparator.comparing(VenueAccessDto::tenantId))
                        .toList());
    }

    @Override
    @Transactional
    public void changePassword(UUID userId, PasswordChangeRequestDto requestDto) {
        StaffUser user = staffUserRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Account not found"));
        if (!passwordEncoder.matches(requestDto.currentPassword(), user.getPasswordHash())) {
            throw new StaffConflictException("The current password is wrong");
        }
        user.setPasswordHash(passwordEncoder.encode(requestDto.newPassword()));
    }
}
