package com.cryptocraft.orderservice.config.converters;

import com.cryptocraft.orderservice.dao.enums.LineStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;


@Converter(autoApply = true)
public class LineStatusConverter implements AttributeConverter<LineStatus, Short> {

    @Override
    public Short convertToDatabaseColumn(LineStatus attribute) {
        return attribute == null ? null : attribute.id().shortValue();
    }

    @Override
    public LineStatus convertToEntityAttribute(Short dbData) {
        return dbData == null ? null : LineStatus.byId(dbData.intValue());
    }
}
