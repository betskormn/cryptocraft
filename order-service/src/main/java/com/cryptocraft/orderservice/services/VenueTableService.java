package com.cryptocraft.orderservice.services;

import com.cryptocraft.orderservice.controllers.dto.request.VenueTableRequestDto;
import com.cryptocraft.orderservice.controllers.dto.response.VenueTableResponseDto;

import java.util.List;

public interface VenueTableService {

    VenueTableResponseDto createTable(VenueTableRequestDto requestDto);

    List<VenueTableResponseDto> getAllTables();

    VenueTableResponseDto updateTable(Long id, VenueTableRequestDto requestDto);
}
