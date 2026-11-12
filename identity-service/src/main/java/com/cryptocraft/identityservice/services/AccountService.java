package com.cryptocraft.identityservice.services;

import com.cryptocraft.identityservice.controllers.dto.request.PasswordChangeRequestDto;
import com.cryptocraft.identityservice.controllers.dto.response.MeResponseDto;

import java.util.UUID;

public interface AccountService {

    MeResponseDto getMe(UUID userId);

    void changePassword(UUID userId, PasswordChangeRequestDto requestDto);
}
