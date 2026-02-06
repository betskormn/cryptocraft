package com.cryptocraft.inventoryservice.mappers;

import com.cryptocraft.inventoryservice.controllers.dto.request.ItemRequestDto;
import com.cryptocraft.inventoryservice.dao.Item;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ItemRequestMapper {

    @Mapping(target = "itemId", ignore = true)
    @Mapping(target = "productLinks", ignore = true)
    Item toEntity(ItemRequestDto dto);

    @Mapping(target = "productLinks", ignore = true)
    @Mapping(target = "itemId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(ItemRequestDto dto, @MappingTarget Item entity);
}