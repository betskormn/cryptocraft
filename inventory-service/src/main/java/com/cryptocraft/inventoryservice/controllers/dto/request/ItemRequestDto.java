package com.cryptocraft.inventoryservice.controllers.dto.request;

import com.cryptocraft.inventoryservice.dao.enums.Type;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public record ItemRequestDto(
        @NotBlank(message = "Item name must not be blank")
        @Size(max = 200, message = "Item name must be at most 200 characters")
        String name,

        @Size(max = 2000, message = "Recipe must be at most 2000 characters")
        String recipe,

        @Size(max = 500, message = "Image URL must be at most 500 characters")
        String image,

        @NotNull(message = "Type must not be null")
        Type type,

        @NotEmpty(message = "Products must not be empty")
        List<@Valid ItemProductDto> products
) {
    public ItemRequestDto {
        products = (products == null) ? List.of() : List.copyOf(products);
    }

    public record ItemProductDto(
            @NotNull(message = "productId must not be null")
            Long productId,

            @NotNull(message = "quantity must not be null")
            @Positive(message = "quantity must be > 0")
            Integer quantity
    ) {}
}
