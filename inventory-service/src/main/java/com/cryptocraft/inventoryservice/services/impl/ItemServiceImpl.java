package com.cryptocraft.inventoryservice.services.impl;

import com.cryptocraft.inventoryservice.controllers.dto.request.ItemRequestDto;
import com.cryptocraft.inventoryservice.controllers.dto.response.BrandResponseDto;
import com.cryptocraft.inventoryservice.controllers.dto.response.ItemResponseDto;
import com.cryptocraft.inventoryservice.controllers.dto.response.ProductResponseDto;
import com.cryptocraft.inventoryservice.dao.Brand;
import com.cryptocraft.inventoryservice.dao.Item;
import com.cryptocraft.inventoryservice.dao.Product;
import com.cryptocraft.inventoryservice.repositories.ItemRepository;
import com.cryptocraft.inventoryservice.repositories.ProductRepository;
import com.cryptocraft.inventoryservice.services.ItemService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.glasnost.orika.MapperFacade;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.format;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;
    private final MapperFacade mapperFacade;
    private final ProductRepository productRepository;


    @Override
    public ItemResponseDto createItem(ItemRequestDto requestDto) {
        Item item = mapperFacade.map(requestDto, Item.class);
        List<Product> managedProducts = requestDto.getProducts().stream()
                .map(p -> productRepository.findById(p.getProductId())
                        .orElseThrow(() -> new RuntimeException("Product not found: " + p.getProductId())))
                .collect(Collectors.toList());

        item.setProducts(managedProducts);
        Item savedItem = itemRepository.save(item);
        log.info("Item with id:{}, successfully created", savedItem.getItemId());
        return mapperFacade.map(item, ItemResponseDto.class);
    }

    @Override
    public ItemResponseDto getItemById(Long id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(format("Item with id:%s, not founded", id)));
        return mapperFacade.map(item, ItemResponseDto.class);
    }

    @Override
    public List<ItemResponseDto> getAllItems() {
        List <Item> items = itemRepository.findAll();
        List<ItemResponseDto> list = itemRepository.findAll()
                .stream()
                .map(item -> mapperFacade.map(item, ItemResponseDto.class))
                .toList();
        return list;
    }

    @Override
    public ItemResponseDto updateItemById(Long id, ItemRequestDto requestDto) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(format("Item with id:%s, not founded", id)));
        mapperFacade.map(requestDto, item);
        return mapperFacade.map(itemRepository.save(item), ItemResponseDto.class);
    }

    @Override
    public void deleteItemById(Long id) {
        itemRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(format("Item with id:%s not founded", id)));
        itemRepository.deleteById(id);
        log.info("Item with id:{} successfully deleted", id);
    }
}
