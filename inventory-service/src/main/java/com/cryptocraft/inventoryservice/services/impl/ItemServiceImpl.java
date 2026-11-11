package com.cryptocraft.inventoryservice.services.impl;

import com.cryptocraft.inventoryservice.controllers.dto.request.ItemRequestDto;
import com.cryptocraft.inventoryservice.controllers.dto.response.ItemPriceResponceDto;
import com.cryptocraft.inventoryservice.controllers.dto.response.ItemResponseDto;
import com.cryptocraft.inventoryservice.dao.Item;
import com.cryptocraft.inventoryservice.dao.ItemProduct;
import com.cryptocraft.inventoryservice.dao.Product;
import com.cryptocraft.inventoryservice.exceptions.ProductNotFoundException;
import com.cryptocraft.inventoryservice.mappers.ItemMapper;
import com.cryptocraft.inventoryservice.mappers.ItemRequestMapper;
import com.cryptocraft.inventoryservice.repositories.ItemPriceRepository;
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
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.String.format;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;
    private final ItemPriceRepository itemPriceRepository;
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
        syncProducts(item, dto);
        Item saved = itemRepository.save(item);
        Item savedWithProducts = itemRepository.findByIdWithProducts(saved.getItemId())
                .orElseThrow(() -> new EntityNotFoundException("Item not found after save: " + saved.getItemId()));
        return itemMapper.toDto(savedWithProducts);
    }

    private void syncProducts(Item item, ItemRequestDto dto) {
        Map<Long, ItemProduct> existing = item.getProductLinks().stream()
                .collect(Collectors.toMap(
                        link -> link.getProduct().getProductId(),
                        Function.identity()
                ));
        List<ItemRequestDto.ItemProductDto> incoming = dto.products();
        Map<Long, Product> products = productRepository.findAllById(
                        incoming.stream()
                                .map(ItemRequestDto.ItemProductDto::productId)
                                .toList()
                ).stream()
                .collect(Collectors.toMap(
                        Product::getProductId,
                        Function.identity()
                ));
        incoming.forEach(p -> {
            ItemProduct existingLink = existing.remove(p.productId());
            if (existingLink != null) {
                existingLink.setQuantity(p.quantity());
                return;
            }
            Product product = products.get(p.productId());
            if (product == null) {
                throw new EntityNotFoundException("Product not found: " + p.productId());
            }
            ItemProduct newLink = ItemProduct.of(item, product, p.quantity());
            item.getProductLinks().add(newLink);
        });
        existing.values().forEach(item::removeLink);
    }

    @Override
    @Transactional
    public void deleteItemById(Long id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(format("Item with id:%s not founded", id)));
        itemRepository.delete(item);
        log.info("Item with id:{} successfully deleted", id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItemPriceResponceDto> getAllPrices() {
        return itemPriceRepository.findAllPrices();
    }
}