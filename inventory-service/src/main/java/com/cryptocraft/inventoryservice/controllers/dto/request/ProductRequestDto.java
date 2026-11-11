package com.cryptocraft.inventoryservice.controllers.dto.request;

import com.cryptocraft.inventoryservice.annotations.BrandExists;
import com.cryptocraft.inventoryservice.dao.enums.Unit;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductRequestDto(

        @NotBlank(message = "Product name must not be blank")
        @Size(max = 200, message = "Product name must be at most 200 characters")
        String name,

        @NotBlank(message = "Description must not be blank")
        @Size(max = 2000, message = "Description must be at most 2000 characters")
        String description,

        @Size(max = 500, message = "Image URL must be at most 500 characters")
        String image,

        // initial stock on create; ignored on update (stock changes via PATCH /stock)
        @NotNull(message = "Quantity must not be null")
        @PositiveOrZero(message = "Quantity must be >= 0")
        @Digits(integer = 9, fraction = 3, message = "Quantity may have at most 3 decimals")
        BigDecimal quantity,

        @NotNull(message = "Unit must not be null")
        Unit unit,

        @NotNull(message = "brandId must not be null")
        @BrandExists
        Long brandId
) {}