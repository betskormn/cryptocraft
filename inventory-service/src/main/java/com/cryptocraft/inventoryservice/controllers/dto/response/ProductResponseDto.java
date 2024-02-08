package com.cryptocraft.inventoryservice.controllers.dto.response;

import com.cryptocraft.inventoryservice.dao.enums.Unit;
import lombok.Data;

@Data
public class ProductResponseDto {

    private Long productId;
    private String name;
    private String description;
    private String image;
    private int quantityInStock;
    private Unit unit;
    private Long brandId;
}
