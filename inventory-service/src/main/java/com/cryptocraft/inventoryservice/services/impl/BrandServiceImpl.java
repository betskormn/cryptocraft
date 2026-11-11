package com.cryptocraft.inventoryservice.services.impl;

import com.cryptocraft.inventoryservice.controllers.dto.request.BrandRequestDto;
import com.cryptocraft.inventoryservice.controllers.dto.response.BrandResponseDto;
import com.cryptocraft.inventoryservice.dao.Brand;
import com.cryptocraft.inventoryservice.exceptions.BrandAlreadyExistsException;
import com.cryptocraft.inventoryservice.exceptions.EntityInUseException;
import com.cryptocraft.inventoryservice.mappers.BrandMapper;
import com.cryptocraft.inventoryservice.repositories.BrandRepository;
import com.cryptocraft.inventoryservice.repositories.ProductRepository;
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
    private final ProductRepository productRepository;

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

    @Transactional(readOnly = true)
    public List<BrandResponseDto> getAllBrands() {
        return brandRepository.findAll()
                .stream()
                .map(brandMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public BrandResponseDto getBrandById(Long id) {
        return brandMapper.toDto(findBrand(id));
    }

    @Transactional
    public BrandResponseDto updateBrand(Long id, BrandRequestDto requestDto) {
        Brand brand = findBrand(id);
        String normalizedName = requestDto.name().trim();
        if (brandRepository.existsByNameIgnoreCaseAndBrandIdNot(normalizedName, id)) {
            throw new BrandAlreadyExistsException(normalizedName);
        }
        brand.setName(normalizedName);
        log.info("Brand with id:{} successfully renamed", id);
        return brandMapper.toDto(brandRepository.save(brand));
    }

    @Transactional
    public void deleteBrand(Long id) {
        Brand brand = findBrand(id);
        long products = productRepository.countByBrand_BrandId(id);
        if (products > 0) {
            throw new EntityInUseException(format("Brand '%s' has %d product(s). Delete them or move them to another brand first",
                    brand.getName(), products));
        }
        brandRepository.delete(brand);
        log.info("Brand with id:{} successfully deleted", id);
    }

    private Brand findBrand(Long id) {
        return brandRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(format("Brand with id:%s, not founded", id)));
    }
}
