package com.cryptocraft.inventoryservice.services;

import com.cryptocraft.inventoryservice.controllers.dto.request.TenantRequestDto;
import com.cryptocraft.inventoryservice.controllers.dto.request.TenantUpdateRequestDto;
import com.cryptocraft.inventoryservice.controllers.dto.response.TenantResponseDto;

import java.util.Collection;
import java.util.List;

public interface TenantService {

    TenantResponseDto createTenant(TenantRequestDto requestDto);

    List<TenantResponseDto> getAllTenants();

    // venues a staff member works in, by name
    List<TenantResponseDto> getTenantsByIds(Collection<Long> ids);

    TenantResponseDto getTenantById(Long id);

    TenantResponseDto getTenantBySlug(String slug);

    TenantResponseDto updateTenant(Long id, TenantUpdateRequestDto requestDto);
}
