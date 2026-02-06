package com.cryptocraft.inventoryservice.services.impl;

import com.cryptocraft.inventoryservice.controllers.dto.request.ItemRequestDto;
import com.cryptocraft.inventoryservice.controllers.dto.response.ItemResponseDto;
import com.cryptocraft.inventoryservice.dao.Item;
import com.cryptocraft.inventoryservice.dao.Product;
import com.cryptocraft.inventoryservice.exceptions.ProductNotFoundException;
import com.cryptocraft.inventoryservice.mappers.ItemMapper;
import com.cryptocraft.inventoryservice.mappers.ItemRequestMapper;
import com.cryptocraft.inventoryservice.repositories.ItemRepository;
import com.cryptocraft.inventoryservice.repositories.ProductRepository;
import com.cryptocraft.inventoryservice.services.ItemService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;
    private final ProductRepository productRepository;
    private final ItemRequestMapper itemRequestMapper;
    private final ItemMapper itemMapper;

    @Override
    @Transactional
    public ItemResponseDto createItem(ItemRequestDto dto) {
        Item item = itemRequestMapper.toEntity(dto);
        attachProducts(item, dto);
        Item saved = itemRepository.save(item);
        log.info("Item with id:{}, successfully created", saved.getItemId());
        Item savedWithProducts = itemRepository.findByIdWithProducts(saved.getItemId())
                .orElseThrow(() -> new EntityNotFoundException("Item not found after save: " + saved.getItemId()));
        return itemMapper.toDto(savedWithProducts);
    }

    private void attachProducts(Item item, ItemRequestDto dto) {
        Map<Long, Integer> qtyByProductId = dto.products().stream()
                .collect(Collectors.groupingBy(
                        ItemRequestDto.ItemProductDto::productId,
                        Collectors.summingInt(ItemRequestDto.ItemProductDto::quantity)
                ));
        List<Long> ids = new ArrayList<>(qtyByProductId.keySet());
        List<Product> products = productRepository.findAllById(ids);
        if (products.size() != ids.size()) {
            Set<Long> found = products.stream()
                    .map(Product::getProductId)
                    .collect(Collectors.toSet());
            ids.removeAll(found);
            throw new ProductNotFoundException(ids);
        }
        products.forEach(product ->
                item.addProduct(product, qtyByProductId.get(product.getProductId()))
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ItemResponseDto getItemById(Long id) {
        Item item = itemRepository.findByIdWithProducts(id)
                .orElseThrow(() -> new EntityNotFoundException(format("Item with id:%s, not founded", id)));
        return itemMapper.toDto(item);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItemResponseDto> getAllItems() {
        List<Item> items = itemRepository.findAllWithProducts();
        return items.stream()
                .map(itemMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public ItemResponseDto updateItemById(Long id, ItemRequestDto dto) {
        Item item = itemRepository.findByIdWithProducts(id)
                .orElseThrow(() -> new EntityNotFoundException(format("Item with id:%s, not founded", id)));
        itemRequestMapper.updateEntity(dto, item);
        item.getProductLinks().clear();
        attachProducts(item, dto);
        Item saved = itemRepository.save(item);
        Item savedWithProducts = itemRepository.findByIdWithProducts(saved.getItemId())
                .orElseThrow(() -> new EntityNotFoundException("Item not found after save: " + saved.getItemId()));
        return itemMapper.toDto(savedWithProducts);
    }

    @Override
    @Transactional
    public void deleteItemById(Long id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(format("Item with id:%s not founded", id)));
        itemRepository.delete(item);
        log.info("Item with id:{} successfully deleted", id);
    }
}