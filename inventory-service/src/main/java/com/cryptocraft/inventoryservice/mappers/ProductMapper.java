package com.cryptocraft.inventoryservice.mappers;

import com.cryptocraft.inventoryservice.controllers.dto.request.ProductRequestDto;
import com.cryptocraft.inventoryservice.controllers.dto.response.ProductResponseDto;
import com.cryptocraft.inventoryservice.dao.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(target = "productId", ignore = true)
    @Mapping(target = "brand", ignore = true)
    @Mapping(target = "itemLinks", ignore = true)
    @Mapping(source = "quantity", target = "quantityInStock")
    @Mapping(target = "tenantId", ignore = true)
    Product toEntity(ProductRequestDto dto);

    @Mapping(target = "productId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    // stock changes only through PATCH /stock
    @Mapping(target = "quantityInStock", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "brand", ignore = true)
    @Mapping(target = "itemLinks", ignore = true)
    void updateEntity(ProductRequestDto dto, @MappingTarget Product entity);

    @Mapping(source = "brand.brandId", target = "brandId")
    ProductResponseDto toDto(Product product);
}