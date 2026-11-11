package com.cryptocraft.orderservice.config.converters;

import com.cryptocraft.orderservice.dao.enums.OrderStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;


@Converter(autoApply = true)
public class OrderStatusConverter implements AttributeConverter<OrderStatus, Short> {

    @Override
    public Short convertToDatabaseColumn(OrderStatus attribute) {
        return attribute == null ? null : attribute.id().shortValue();
    }

    @Override
    public OrderStatus convertToEntityAttribute(Short dbData) {
        return dbData == null ? null : OrderStatus.byId(dbData.intValue());
    }
}
