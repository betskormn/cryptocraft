package com.cryptocraft.inventoryservice.controllers.dto.request;

import com.cryptocraft.inventoryservice.dao.enums.Type;
import lombok.*;

import java.util.List;

@Data
public class ItemRequestDto {

    private String name;
    private String recipe;
    private String image;
    private Type type;
    private List<ItemProductDto> products;

    @Data
    public static class ItemProductDto {
        private Long productId;
        private int quantity;
    }
}
