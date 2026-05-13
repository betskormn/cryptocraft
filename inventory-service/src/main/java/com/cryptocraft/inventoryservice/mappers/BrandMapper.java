package com.cryptocraft.inventoryservice.mappers;

import com.cryptocraft.inventoryservice.controllers.dto.request.BrandRequestDto;
import com.cryptocraft.inventoryservice.controllers.dto.response.BrandResponseDto;
import com.cryptocraft.inventoryservice.dao.Brand;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BrandMapper {

    @Mapping(target = "brandId", ignore = true)
    @Mapping(target = "products", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    Brand toEntity(BrandRequestDto dto);

    BrandResponseDto toDto(Brand brand);
}