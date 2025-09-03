package com.cryptocraft.inventoryservice.services.impl;

import com.cryptocraft.inventoryservice.controllers.dto.request.ItemRequestDto;
import com.cryptocraft.inventoryservice.controllers.dto.response.ItemResponseDto;
import com.cryptocraft.inventoryservice.repositories.ItemRepository;
import com.cryptocraft.inventoryservice.services.ItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;


    @Override
    public ItemResponseDto createItem(ItemRequestDto requestDto) {
        return null;
    }

    @Override
    public ItemResponseDto getItemById(Long id) {
        return null;
    }

    @Override
    public List<ItemResponseDto> getAllItems() {
        return null;
    }

    @Override
    public ItemResponseDto updateItemById(Long id, ItemRequestDto requestDto) {
        return null;
    }

    @Override
    public void deleteItemById(Long id) {

    }
}
