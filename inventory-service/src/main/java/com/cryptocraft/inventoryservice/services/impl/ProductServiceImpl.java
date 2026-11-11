package com.cryptocraft.inventoryservice.services.impl;

import com.cryptocraft.inventoryservice.controllers.dto.request.ProductRequestDto;
import com.cryptocraft.inventoryservice.controllers.dto.response.ProductResponseDto;
import com.cryptocraft.inventoryservice.dao.Brand;
import com.cryptocraft.inventoryservice.dao.Product;
import com.cryptocraft.inventoryservice.exceptions.BrandNotFoundException;
import com.cryptocraft.inventoryservice.exceptions.EntityInUseException;
import com.cryptocraft.inventoryservice.exceptions.InsufficientStockException;
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

import java.math.BigDecimal;
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

    @Transactional(readOnly = true)
    public List<ProductResponseDto> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(productMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductResponseDto getProductById(Long id) {
        return productMapper.toDto(findProduct(id));
    }

    /**
     * Stock is not part of a full update: it changes only through {@link #adjustStock}, so an edit
     * made from a stale form cannot overwrite deliveries or write-offs recorded in the meantime.
     */
    @Transactional
    public ProductResponseDto updateProductById(Long id, ProductRequestDto requestDto) {
        Product product = findProduct(id);
        String normalizedName = requestDto.name().trim();
        Long brandId = requestDto.brandId();
        Brand brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new BrandNotFoundException(brandId));
        if (productRepository.existsByNameIgnoreCaseAndBrand_BrandIdAndProductIdNot(normalizedName, brandId, id)) {
            throw new ProductAlreadyExistsException(normalizedName, brandId);
        }
        productMapper.updateEntity(requestDto, product);
        product.setName(normalizedName);
        product.setBrand(brand);
        return productMapper.toDto(productRepository.save(product));
    }

    @Transactional
    public void deleteProductById(Long id) {
        Product product = findProduct(id);
        List<String> items = productRepository.findItemNamesUsingProduct(id);
        if (!items.isEmpty()) {
            throw new EntityInUseException(format("Product '%s' is used in %d item(s): %s. Remove it from their recipes first",
                    product.getName(), items.size(), String.join(", ", items)));
        }
        productRepository.delete(product);
        log.info("Product with id:{} successfully deleted", id);
    }

    @Transactional
    public ProductResponseDto adjustStock(Long productId, BigDecimal delta) {
        if (delta.signum() == 0) {
            throw new IllegalArgumentException("delta must not be 0");
        }
        if (productRepository.incrementStock(productId, delta) == 0) {
            findProduct(productId);
            throw new InsufficientStockException(productId, delta);
        }
        return getProductById(productId);
    }

    private Product findProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(format("Product with id:%s, not founded", id)));
    }
}
