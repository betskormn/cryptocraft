package com.cryptocraft.orderservice.services;

import com.cryptocraft.orderservice.controllers.dto.request.PlaceOrderRequestDto;
import com.cryptocraft.orderservice.controllers.dto.response.OrderResponseDto;

import java.util.UUID;

public interface OrderService {

    /**
     * Sends a guest's cart to the kitchen/bar: prices are locked now and the order goes on the guest's bill.
     * A repeated call with the same clientOrderId returns the first order with {@code created = false}.
     */
    Result placeOrder(UUID participantId, PlaceOrderRequestDto requestDto);

    OrderResponseDto getOrderById(UUID orderId);

    record Result(OrderResponseDto order, boolean created) {}
}
