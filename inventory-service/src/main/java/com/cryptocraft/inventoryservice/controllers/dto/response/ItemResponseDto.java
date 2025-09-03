package com.cryptocraft.inventoryservice.controllers.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class ItemResponseDto {
    private Long itemId;
    private String name;
    private String recipe;
    private String image;
    private String type;
    private List<ProductResponseDto> products;
}
