package com.cryptocraft.orderservice.mappers;

import com.cryptocraft.orderservice.controllers.dto.response.TableSessionResponseDto;
import com.cryptocraft.orderservice.dao.TableSession;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TableSessionMapper {

    @Mapping(target = "tableId", source = "table.tableId")
    @Mapping(target = "tableLabel", source = "table.label")
    @Mapping(target = "openedAt", source = "createdAt")
    TableSessionResponseDto toDto(TableSession session);
}
