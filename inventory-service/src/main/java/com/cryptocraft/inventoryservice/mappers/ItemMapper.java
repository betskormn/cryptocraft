package com.cryptocraft.inventoryservice.mappers;

import com.cryptocraft.inventoryservice.controllers.dto.response.ItemProductResponseDto;
import com.cryptocraft.inventoryservice.controllers.dto.response.ItemResponseDto;
import com.cryptocraft.inventoryservice.dao.Item;
import com.cryptocraft.inventoryservice.dao.ItemProduct;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", uses = ProductMapper.class)
public abstract class ItemMapper {

    @Autowired
    protected ProductMapper productMapper;

    @Mapping(target = "type", expression = "java(item.getType() != null ? item.getType().name() : null)")
    @Mapping(target = "products", expression = "java(mapProducts(item))")
    @Mapping(target = "basePrice", source = "basePrice")
    @Mapping(target = "minPrice", source = "minPrice")
    @Mapping(target = "maxPrice", source = "maxPrice")
    public abstract ItemResponseDto toDto(Item item);

    protected List<ItemProductResponseDto> mapProducts(Item item) {
        List<ItemProduct> links = item.getProductLinks();

        return links == null || links.isEmpty()
                ? List.of()
                : links.stream()
                .map(link -> new ItemProductResponseDto(
                        link.getProduct().getProductId(),
                        link.getProduct().getName(),
                        link.getQuantity()
                ))
                .collect(Collectors.toList());
    }
}