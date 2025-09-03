package com.cryptocraft.inventoryservice.services.impl;

import com.cryptocraft.inventoryservice.controllers.dto.request.ProductRequestDto;
import com.cryptocraft.inventoryservice.controllers.dto.response.ProductResponseDto;
import com.cryptocraft.inventoryservice.dao.Product;
import com.cryptocraft.inventoryservice.repositories.ProductRepository;
import com.cryptocraft.inventoryservice.services.ProductService;
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
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final MapperFacade mapperFacade;


    public ProductResponseDto createProduct(ProductRequestDto productRequestDto) {
        Product product = productRepository.save(mapperFacade.map(productRequestDto, Product.class));
        log.info("Product with id:{}, successfully created", product.getProductId());
        return mapperFacade.map(product, ProductResponseDto.class);
    }

    public List<ProductResponseDto> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(product -> mapperFacade.map(product, ProductResponseDto.class))
                .toList();
    }

    public ProductResponseDto getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(format("Product with id:%s, not founded", id)));
        return mapperFacade.map(product, ProductResponseDto.class);
    }

    public ProductResponseDto updateProductById(Long id, ProductRequestDto requestDto) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(format("Product with id:%s, not founded", id)));
        mapperFacade.map(requestDto, product);
        return mapperFacade.map(productRepository.save(product), ProductResponseDto.class);
    }


    public void deleteProductById(Long id) {
        productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(format("Product with id:%s not founded", id)));
        productRepository.deleteById(id);
        log.info("Product with id:{} successfully deleted", id);
    }
}