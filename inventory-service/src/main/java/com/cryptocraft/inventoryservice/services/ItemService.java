package com.cryptocraft.inventoryservice.services;

import com.cryptocraft.inventoryservice.controllers.dto.request.ItemRequestDto;
import com.cryptocraft.inventoryservice.controllers.dto.response.ItemPriceResponceDto;
import com.cryptocraft.inventoryservice.controllers.dto.response.ItemResponseDto;

import java.util.List;

public interface ItemService {

    ItemResponseDto createItem(ItemRequestDto requestDto);

    ItemResponseDto getItemById(Long id);

    List<ItemResponseDto> getAllItems();

    ItemResponseDto updateItemById(Long id, ItemRequestDto requestDto);

    void deleteItemById(Long id);

    List<ItemPriceResponceDto> getAllPrices();
}
