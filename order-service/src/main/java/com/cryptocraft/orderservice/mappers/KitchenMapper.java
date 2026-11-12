package com.cryptocraft.orderservice.mappers;

import com.cryptocraft.orderservice.controllers.dto.response.KitchenLineResponseDto;
import com.cryptocraft.orderservice.dao.OrderLine;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface KitchenMapper {

    @Mapping(target = "orderId", source = "order.orderId")
    @Mapping(target = "tableLabel", source = "order.session.table.label")
    @Mapping(target = "guestName", source = "order.participant.displayName")
    @Mapping(target = "placedAt", source = "order.sentAt")
    KitchenLineResponseDto toDto(OrderLine line);
}
