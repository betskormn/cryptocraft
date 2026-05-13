package com.cryptocraft.inventoryservice.config.converters;

import com.cryptocraft.inventoryservice.dao.enums.Unit;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class UnitConverter implements AttributeConverter<Unit, Short> {

    @Override
    public Short convertToDatabaseColumn(Unit attribute) {
        return attribute == null ? null : attribute.id().shortValue();
    }

    @Override
    public Unit convertToEntityAttribute(Short dbData) {
        return dbData == null ? null : Unit.byId(dbData.intValue());
    }
}
