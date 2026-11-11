package com.cryptocraft.orderservice.mappers;

import com.cryptocraft.orderservice.controllers.dto.response.OrderLineResponseDto;
import com.cryptocraft.orderservice.controllers.dto.response.OrderResponseDto;
import com.cryptocraft.orderservice.dao.Order;
import com.cryptocraft.orderservice.dao.OrderLine;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(target = "participantId", source = "participant.participantId")
    OrderResponseDto toDto(Order order);

    OrderLineResponseDto toDto(OrderLine line);
}
