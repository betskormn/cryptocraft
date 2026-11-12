package com.cryptocraft.orderservice.services;

import com.cryptocraft.orderservice.controllers.dto.request.LineStatusRequestDto;
import com.cryptocraft.orderservice.controllers.dto.response.KitchenLineResponseDto;
import com.cryptocraft.orderservice.dao.enums.ItemType;

import java.util.List;

public interface KitchenService {

    /**
     * Lines of one station (MEAL for the kitchen, DRINK for the bar) still in work, in the order they were sent.
     */
    List<KitchenLineResponseDto> getQueue(ItemType itemType);

    /**
     * Moves a line on the screen; CANCELLED also takes it off the guest's bill.
     */
    KitchenLineResponseDto changeStatus(Long lineId, LineStatusRequestDto requestDto);
}
