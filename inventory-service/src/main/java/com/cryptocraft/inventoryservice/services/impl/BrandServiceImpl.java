package com.cryptocraft.inventoryservice.services.impl;

import com.cryptocraft.inventoryservice.controllers.dto.request.BrandRequestDto;
import com.cryptocraft.inventoryservice.controllers.dto.response.BrandResponseDto;
import com.cryptocraft.inventoryservice.dao.Brand;
import com.cryptocraft.inventoryservice.exceptions.BrandAlreadyExistsException;
import com.cryptocraft.inventoryservice.mappers.BrandMapper;
import com.cryptocraft.inventoryservice.repositories.BrandRepository;
import com.cryptocraft.inventoryservice.services.BrandService;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static java.lang.String.format;

@Slf4j
@Service
@AllArgsConstructor
public class BrandServiceImpl implements BrandService {

    private final BrandRepository brandRepository;
    private final BrandMapper brandMapper;

    @Transactional
    public BrandResponseDto createBrand(BrandRequestDto brandRequestDto) {
        String normalizedName = brandRequestDto.name().trim();
        boolean exists = brandRepository.existsByNameIgnoreCase(normalizedName);
        if (exists) {
            throw new BrandAlreadyExistsException(normalizedName);
        }
        Brand brandToSave = brandMapper.toEntity(brandRequestDto);
        brandToSave.setName(normalizedName);
        Brand savedBrand = brandRepository.save(brandToSave);
        log.info("Brand with id:{}, successfully created", savedBrand.getBrandId());
        return brandMapper.toDto(savedBrand);
    }

    public List<BrandResponseDto> getAllBrands() {
        return brandRepository.findAll()
                .stream()
                .map(brandMapper::toDto)
                .toList();
    }

    public BrandResponseDto getBrandById(Long id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(format("Brand with id:%s, not founded", id)));
        return brandMapper.toDto(brand);
    }


    public void deleteBrand(Long id) {
        brandRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(format("Brand with id:%s, not founded", id)));
        brandRepository.deleteById(id);
        log.info("Brand with id:{} successfully deleted", id);
    }
}
