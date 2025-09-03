package com.cryptocraft.inventoryservice.controllers;

import com.cryptocraft.inventoryservice.controllers.dto.request.ProductRequestDto;
import com.cryptocraft.inventoryservice.controllers.dto.response.ProductResponseDto;
import com.cryptocraft.inventoryservice.services.ProductService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/inventory/product")
public class ProductController {

    private final ProductService productService;
    @Value("${eureka.instance.instance-id}")
    private String id;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    //todo delete it in future
    @GetMapping("test")
    public String test (){
        return id;
    }

    @PostMapping
    public ProductResponseDto createProduct(@RequestBody @Valid ProductRequestDto requestDto) {
        return productService.createProduct(requestDto);
    }

    @GetMapping
    public List<ProductResponseDto> getAllProducts() {
        return productService.getAllProducts();
    }

    @GetMapping("/{id}")
    public ProductResponseDto getProductById(@PathVariable Long id) {
        return productService.getProductById(id);
    }

    @PutMapping("/{id}")
    public ProductResponseDto updateProductById(@PathVariable Long id, @RequestBody @Valid ProductRequestDto requestDto) {
        return productService.updateProductById(id, requestDto);
    }

    @DeleteMapping("/{id}")
    public void deleteProduct(@PathVariable Long id) {
        productService.deleteProductById(id);
    }
}