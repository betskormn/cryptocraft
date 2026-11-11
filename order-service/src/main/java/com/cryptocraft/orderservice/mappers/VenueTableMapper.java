package com.cryptocraft.orderservice.mappers;

import com.cryptocraft.orderservice.controllers.dto.response.VenueTableResponseDto;
import com.cryptocraft.orderservice.dao.VenueTable;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface VenueTableMapper {

    VenueTableResponseDto toDto(VenueTable table);
}
