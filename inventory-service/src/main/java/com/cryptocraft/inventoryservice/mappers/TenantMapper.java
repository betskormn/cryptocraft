package com.cryptocraft.inventoryservice.mappers;

import com.cryptocraft.inventoryservice.controllers.dto.response.TenantResponseDto;
import com.cryptocraft.inventoryservice.dao.Tenant;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TenantMapper {

    TenantResponseDto toDto(Tenant tenant);
}
