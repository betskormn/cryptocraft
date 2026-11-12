package com.cryptocraft.orderservice.config.converters;

import com.cryptocraft.orderservice.dao.enums.GuestStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;


@Converter(autoApply = true)
public class GuestStatusConverter implements AttributeConverter<GuestStatus, Short> {

    @Override
    public Short convertToDatabaseColumn(GuestStatus attribute) {
        return attribute == null ? null : attribute.id().shortValue();
    }

    @Override
    public GuestStatus convertToEntityAttribute(Short dbData) {
        return dbData == null ? null : GuestStatus.byId(dbData.intValue());
    }
}
