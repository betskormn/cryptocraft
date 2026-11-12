package com.cryptocraft.identityservice.services;

import com.cryptocraft.identityservice.controllers.dto.request.StaffRequestDto;
import com.cryptocraft.identityservice.controllers.dto.request.StaffRoleRequestDto;
import com.cryptocraft.identityservice.controllers.dto.response.StaffResponseDto;

import java.util.List;
import java.util.UUID;

public interface StaffService {

    List<StaffResponseDto> getStaff(Long tenantId);

    StaffResponseDto addStaff(Long tenantId, StaffRequestDto requestDto);

    StaffResponseDto changeRole(Long tenantId, UUID userId, StaffRoleRequestDto requestDto);

    void removeStaff(Long tenantId, UUID userId);
}
