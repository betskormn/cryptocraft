package com.cryptocraft.inventoryservice.mappers;

import com.cryptocraft.inventoryservice.controllers.dto.response.ItemResponseDto;
import com.cryptocraft.inventoryservice.controllers.dto.response.ProductResponseDto;
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
    public abstract ItemResponseDto toDto(Item item);

    protected List<ProductResponseDto> mapProducts(Item item) {
        List<ItemProduct> links = item.getProductLinks();
        return links == null || links.isEmpty() ? List.of() : links.stream()
                .map(ItemProduct::getProduct)
                .filter(Objects::nonNull)
                .map(product -> productMapper.toDto(product))
                .collect(Collectors.toCollection(() -> new ArrayList<>(links.size())));
    }
}