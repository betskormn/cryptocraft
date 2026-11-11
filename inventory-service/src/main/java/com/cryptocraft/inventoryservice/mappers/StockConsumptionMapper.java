package com.cryptocraft.inventoryservice.mappers;

import com.cryptocraft.inventoryservice.controllers.dto.response.StockConsumptionResponseDto;
import com.cryptocraft.inventoryservice.dao.StockConsumption;
import com.cryptocraft.inventoryservice.dao.StockConsumptionLine;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StockConsumptionMapper {

    @Mapping(target = "hasShortages", expression = "java(consumption.getLines().stream()"
            + ".anyMatch(line -> line.getConsumed().compareTo(line.getRequested()) < 0))")
    StockConsumptionResponseDto toDto(StockConsumption consumption);

    @Mapping(target = "shortage", expression = "java(line.getRequested().subtract(line.getConsumed()))")
    StockConsumptionResponseDto.Line toDto(StockConsumptionLine line);
}
