package com.cryptocraft.orderservice.config.converters;

import com.cryptocraft.orderservice.dao.enums.StockStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;


@Converter(autoApply = true)
public class StockStatusConverter implements AttributeConverter<StockStatus, Short> {

    @Override
    public Short convertToDatabaseColumn(StockStatus attribute) {
        return attribute == null ? null : attribute.id().shortValue();
    }

    @Override
    public StockStatus convertToEntityAttribute(Short dbData) {
        return dbData == null ? null : StockStatus.byId(dbData.intValue());
    }
}
