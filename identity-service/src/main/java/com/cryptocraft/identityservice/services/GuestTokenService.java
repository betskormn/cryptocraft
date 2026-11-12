package com.cryptocraft.identityservice.services;

import com.cryptocraft.identityservice.controllers.dto.request.GuestTokenRequestDto;
import com.cryptocraft.identityservice.controllers.dto.response.GuestTokenResponseDto;

public interface GuestTokenService {

    GuestTokenResponseDto issue(GuestTokenRequestDto requestDto);
}
