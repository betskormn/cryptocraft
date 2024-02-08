package com.cryptocraft.inventoryservice.controllers.dto.request;

import com.cryptocraft.inventoryservice.annotations.BrandExists;
import com.cryptocraft.inventoryservice.dao.enums.Unit;
import lombok.Data;


@Data
public class ProductRequestDto {

    private String name;
    private String description;
    private String image;
    private int quantity;
    private Unit unit;
    @BrandExists
    private Long brandId;
}
