package com.cryptocraft.inventoryservice.config.converters;

import com.cryptocraft.inventoryservice.dao.enums.Type;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;


@Converter(autoApply = true)
public class TypeConverter implements AttributeConverter<Type, Integer> {

    @Override
    public Integer convertToDatabaseColumn(Type attribute) {
        return attribute.id();
    }

    @Override
    public Type convertToEntityAttribute(Integer dbData) {
        return Type.byId(dbData);
    }
}
