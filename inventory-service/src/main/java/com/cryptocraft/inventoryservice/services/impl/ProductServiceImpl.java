package com.cryptocraft.inventoryservice.services.impl;

import com.cryptocraft.inventoryservice.controllers.dto.request.ProductRequestDto;
import com.cryptocraft.inventoryservice.controllers.dto.response.ProductResponseDto;
import com.cryptocraft.inventoryservice.dao.Brand;
import com.cryptocraft.inventoryservice.dao.Product;
import com.cryptocraft.inventoryservice.exceptions.BrandNotFoundException;
import com.cryptocraft.inventoryservice.exceptions.ProductAlreadyExistsException;
import com.cryptocraft.inventoryservice.mappers.ProductMapper;
import com.cryptocraft.inventoryservice.repositories.BrandRepository;
import com.cryptocraft.inventoryservice.repositories.ProductRepository;
import com.cryptocraft.inventoryservice.services.ProductService;
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
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final ProductMapper productMapper;


    @Transactional
    public ProductResponseDto createProduct(ProductRequestDto productRequestDto) {
        String normalizedName = productRequestDto.name().trim();
        Long brandId = productRequestDto.brandId();
        Brand brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new BrandNotFoundException(brandId));

        boolean exists = productRepository.existsByNameIgnoreCaseAndBrand_BrandId(normalizedName, brandId);
        if (exists) {
            throw new ProductAlreadyExistsException(normalizedName, brandId);
        }

        Product productToSave = productMapper.toEntity(productRequestDto);
        productToSave.setName(normalizedName);
        productToSave.setBrand(brand);

        Product savedProduct = productRepository.save(productToSave);
        log.info("Product with id:{}, successfully created", savedProduct.getProductId());
        return productMapper.toDto(savedProduct);
    }


    public List<ProductResponseDto> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(productMapper::toDto)
                .toList();
    }

    public ProductResponseDto getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(format("Product with id:%s, not founded", id)));
        return productMapper.toDto(product);
    }

    public ProductResponseDto updateProductById(Long id, ProductRequestDto requestDto) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(format("Product with id:%s, not founded", id)));
        productMapper.toEntity(requestDto);
        return productMapper.toDto(productRepository.save(product));
    }


    public void deleteProductById(Long id) {
        productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(format("Product with id:%s not founded", id)));
        productRepository.deleteById(id);
        log.info("Product with id:{} successfully deleted", id);
    }
}