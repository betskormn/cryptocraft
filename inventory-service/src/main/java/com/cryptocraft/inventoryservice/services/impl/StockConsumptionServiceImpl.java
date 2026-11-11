package com.cryptocraft.inventoryservice.services.impl;

import com.cryptocraft.inventoryservice.controllers.dto.request.StockConsumptionRequestDto;
import com.cryptocraft.inventoryservice.controllers.dto.response.StockConsumptionResponseDto;
import com.cryptocraft.inventoryservice.dao.Item;
import com.cryptocraft.inventoryservice.dao.Product;
import com.cryptocraft.inventoryservice.dao.StockConsumption;
import com.cryptocraft.inventoryservice.mappers.StockConsumptionMapper;
import com.cryptocraft.inventoryservice.repositories.ItemRepository;
import com.cryptocraft.inventoryservice.repositories.ProductRepository;
import com.cryptocraft.inventoryservice.repositories.StockConsumptionRepository;
import com.cryptocraft.inventoryservice.repositories.projections.RecipeLine;
import com.cryptocraft.inventoryservice.services.StockConsumptionService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static java.lang.String.format;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockConsumptionServiceImpl implements StockConsumptionService {

    private final StockConsumptionRepository stockConsumptionRepository;
    private final ItemRepository itemRepository;
    private final ProductRepository productRepository;
    private final StockConsumptionMapper stockConsumptionMapper;

    @Override
    @Transactional
    public Result consume(StockConsumptionRequestDto dto) {
        Optional<StockConsumption> existing = stockConsumptionRepository.findByOrderIdWithLines(dto.orderId());
        if (existing.isPresent()) {
            log.info("Stock for order:{} was already written off", dto.orderId());
            return new Result(stockConsumptionMapper.toDto(existing.get()), false);
        }

        Map<Long, Integer> portionsByItem = dto.lines().stream()
                .collect(Collectors.groupingBy(
                        StockConsumptionRequestDto.Line::itemId,
                        Collectors.summingInt(StockConsumptionRequestDto.Line::quantity)
                ));
        requireItems(portionsByItem.keySet());

        // product id -> total needed; sorted so rows are locked in a stable order
        Map<Long, BigDecimal> required = new TreeMap<>();
        for (RecipeLine line : itemRepository.findRecipeLines(portionsByItem.keySet())) {
            BigDecimal amount = line.quantity().multiply(BigDecimal.valueOf(portionsByItem.get(line.itemId())));
            required.merge(line.productId(), amount, BigDecimal::add);
        }

        StockConsumption consumption = StockConsumption.builder()
                .orderId(dto.orderId())
                .build();
        for (Product product : productRepository.findAllByIdForUpdate(required.keySet())) {
            BigDecimal requested = required.get(product.getProductId()).setScale(3, RoundingMode.HALF_UP);
            // never below zero: the order goes ahead, the shortage is reported instead
            BigDecimal consumed = requested.min(product.getQuantityInStock());
            product.setQuantityInStock(product.getQuantityInStock().subtract(consumed));
            consumption.addLine(product, requested, consumed);
        }
        StockConsumption saved = stockConsumptionRepository.save(consumption);

        StockConsumptionResponseDto response = stockConsumptionMapper.toDto(saved);
        if (response.hasShortages()) {
            log.warn("Stock for order:{} written off with shortages", dto.orderId());
        } else {
            log.info("Stock for order:{} successfully written off", dto.orderId());
        }
        return new Result(response, true);
    }

    @Override
    @Transactional(readOnly = true)
    public StockConsumptionResponseDto getByOrderId(String orderId) {
        return stockConsumptionRepository.findByOrderIdWithLines(orderId)
                .map(stockConsumptionMapper::toDto)
                .orElseThrow(() -> new EntityNotFoundException(format("Stock consumption for order:%s, not founded", orderId)));
    }

    private void requireItems(Set<Long> itemIds) {
        Set<Long> missing = new HashSet<>(itemIds);
        itemRepository.findAllById(itemIds).stream()
                .map(Item::getItemId)
                .forEach(missing::remove);
        if (!missing.isEmpty()) {
            throw new EntityNotFoundException("Items not found: " + missing);
        }
    }
}
