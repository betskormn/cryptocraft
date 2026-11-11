package com.cryptocraft.inventoryservice.services;

import com.cryptocraft.inventoryservice.controllers.dto.request.BrandRequestDto;
import com.cryptocraft.inventoryservice.controllers.dto.response.BrandResponseDto;

import java.util.List;

public interface BrandService {

    BrandResponseDto createBrand(BrandRequestDto item);

    List<BrandResponseDto> getAllBrands();

    BrandResponseDto getBrandById(Long id);

    BrandResponseDto updateBrand(Long id, BrandRequestDto requestDto);

    void deleteBrand(Long id);
}