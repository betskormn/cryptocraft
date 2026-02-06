package com.cryptocraft.inventoryservice.mappers;

import com.cryptocraft.inventoryservice.controllers.dto.request.ProductRequestDto;
import com.cryptocraft.inventoryservice.controllers.dto.response.ProductResponseDto;
import com.cryptocraft.inventoryservice.dao.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(target = "productId", ignore = true)
    @Mapping(target = "brand", ignore = true) // brand ставимо в сервісі (managed entity з БД)
    @Mapping(target = "itemLinks", ignore = true)
    @Mapping(source = "quantity", target = "quantityInStock")
    Product toEntity(ProductRequestDto dto);

    @Mapping(source = "brand.brandId", target = "brandId")
    ProductResponseDto toDto(Product product);
}