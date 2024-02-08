package com.cryptocraft.inventoryservice.services.impl;

import com.cryptocraft.inventoryservice.controllers.dto.request.BrandRequestDto;
import com.cryptocraft.inventoryservice.controllers.dto.response.BrandResponseDto;
import com.cryptocraft.inventoryservice.dao.Brand;
import com.cryptocraft.inventoryservice.repositories.BrandRepository;
import com.cryptocraft.inventoryservice.services.BrandService;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.glasnost.orika.MapperFacade;
import org.springframework.stereotype.Service;

import java.util.List;

import static java.lang.String.format;

@Slf4j
@Service
@AllArgsConstructor
public class BrandServiceImpl implements BrandService {

    private final BrandRepository brandRepository;
    private final MapperFacade mapperFacade;

    public BrandResponseDto createBrand(BrandRequestDto brandRequestDto) {
        Brand brand = brandRepository.save(mapperFacade.map(brandRequestDto, Brand.class));
        log.info("Brand with id:{}, successfully created", brand.getBrandId());
        return mapperFacade.map(brand, BrandResponseDto.class);
    }

    public List<BrandResponseDto> getAllBrands() {
        return brandRepository.findAll()
                .stream()
                .map(brand -> mapperFacade.map(brand, BrandResponseDto.class))
                .toList();
    }

    public BrandResponseDto getBrandById(Long id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(format("Brand with id:%s, not founded", id)));
        return mapperFacade.map(brand, BrandResponseDto.class);
    }


    public void deleteBrand(Long id) {
        brandRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(format("Brand with id:%s, not founded", id)));
        brandRepository.deleteById(id);
        log.info("Brand with id:{} successfully deleted", id);
    }
}
