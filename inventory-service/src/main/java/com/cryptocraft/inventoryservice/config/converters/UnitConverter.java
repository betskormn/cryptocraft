package com.cryptocraft.inventoryservice.config.converters;

import com.cryptocraft.inventoryservice.dao.enums.Unit;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class UnitConverter implements AttributeConverter<Unit, Integer> {

    @Override
    public Integer convertToDatabaseColumn(Unit attribute) {
        return attribute.id();
    }

    @Override
    public Unit convertToEntityAttribute(Integer dbData) {
        return Unit.byId(dbData);
    }
}
