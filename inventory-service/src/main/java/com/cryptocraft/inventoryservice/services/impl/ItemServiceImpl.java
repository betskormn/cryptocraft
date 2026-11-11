package com.cryptocraft.inventoryservice.services.impl;

import com.cryptocraft.inventoryservice.controllers.dto.request.ItemRequestDto;
import com.cryptocraft.inventoryservice.controllers.dto.response.ItemPriceResponceDto;
import com.cryptocraft.inventoryservice.controllers.dto.response.ItemResponseDto;
import com.cryptocraft.inventoryservice.controllers.dto.response.SellableItemResponseDto;
import com.cryptocraft.inventoryservice.dao.Item;
import com.cryptocraft.inventoryservice.dao.ItemProduct;
import com.cryptocraft.inventoryservice.dao.Product;
import com.cryptocraft.inventoryservice.events.ItemChangedEvent;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
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
    // pricing and TV screens are notified after commit (ItemConfigPublisher)
    private final ApplicationEventPublisher eventPublisher;

    // items with a display order first (ascending), then the rest by name
    private static final Comparator<Item> MENU_ORDER = Comparator
            .comparing(Item::getDisplayOrder, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(Item::getName, String.CASE_INSENSITIVE_ORDER);

    @Override
    @Transactional
    public ItemResponseDto createItem(ItemRequestDto dto) {
        Item item = itemRequestMapper.toEntity(dto);
        attachProducts(item, dto);
        Item saved = itemRepository.save(item);
        eventPublisher.publishEvent(new ItemChangedEvent(saved.getItemId()));
        log.info("Item with id:{}, successfully created", saved.getItemId());
        Item savedWithProducts = itemRepository.findByIdWithProducts(saved.getItemId())
                .orElseThrow(() -> new EntityNotFoundException("Item not found after save: " + saved.getItemId()));
        return itemMapper.toDto(savedWithProducts);
    }

    private void attachProducts(Item item, ItemRequestDto dto) {
        Map<Long, BigDecimal> qtyByProductId = quantitiesByProduct(dto);
        loadProducts(qtyByProductId.keySet()).forEach((productId, product) ->
                item.addProduct(product, qtyByProductId.get(productId))
        );
    }

    // the same product listed twice in a request is one recipe line with the summed quantity
    private static Map<Long, BigDecimal> quantitiesByProduct(ItemRequestDto dto) {
        return dto.products().stream()
                .collect(Collectors.groupingBy(
                        ItemRequestDto.ItemProductDto::productId,
                        Collectors.reducing(BigDecimal.ZERO, ItemRequestDto.ItemProductDto::quantity, BigDecimal::add)
                ));
    }

    private Map<Long, Product> loadProducts(Set<Long> productIds) {
        Map<Long, Product> products = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getProductId, Function.identity()));
        if (products.size() != productIds.size()) {
            List<Long> missing = new ArrayList<>(productIds);
            missing.removeAll(products.keySet());
            throw new ProductNotFoundException(missing);
        }
        return products;
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
        eventPublisher.publishEvent(new ItemChangedEvent(saved.getItemId()));
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
        Map<Long, BigDecimal> qtyByProductId = quantitiesByProduct(dto);
        Map<Long, Product> products = loadProducts(qtyByProductId.keySet());

        qtyByProductId.forEach((productId, quantity) -> {
            ItemProduct existingLink = existing.remove(productId);
            if (existingLink != null) {
                existingLink.setQuantity(quantity);
            } else {
                item.getProductLinks().add(ItemProduct.of(item, products.get(productId), quantity));
            }
        });
        existing.values().forEach(item::removeLink);
    }

    @Override
    @Transactional
    public void deleteItemById(Long id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(format("Item with id:%s not founded", id)));
        itemRepository.delete(item);
        eventPublisher.publishEvent(new ItemChangedEvent(id));
        log.info("Item with id:{} successfully deleted", id);
    }

    @Override
    @Transactional
    public ItemResponseDto setStopped(Long id, boolean stopped) {
        Item item = itemRepository.findByIdWithProducts(id)
                .orElseThrow(() -> new EntityNotFoundException(format("Item with id:%s, not founded", id)));
        item.setStopped(stopped);
        eventPublisher.publishEvent(new ItemChangedEvent(id));
        log.info("Item with id:{} {} the stop-list", id, stopped ? "added to" : "removed from");
        return itemMapper.toDto(itemRepository.save(item));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SellableItemResponseDto> getSellableItems() {
        return itemRepository.findAllWithProducts().stream()
                .sorted(MENU_ORDER)
                .map(item -> new SellableItemResponseDto(
                        item.getItemId(),
                        item.getName(),
                        item.getType() != null ? item.getType().name() : null,
                        item.getImage(),
                        item.getDisplayOrder(),
                        !item.isStopped(),
                        estimatePortions(item)))
                .toList();
    }

    // how many times the recipe fits into current stock; null when the item has no recipe
    private static Integer estimatePortions(Item item) {
        return item.getProductLinks().stream()
                .map(link -> link.getProduct().getQuantityInStock()
                        .divideToIntegralValue(link.getQuantity())
                        .intValue())
                .min(Integer::compare)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItemPriceResponceDto> getAllPrices() {
        return itemPriceRepository.findAllPrices();
    }
}