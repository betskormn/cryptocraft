package com.cryptocraft.inventoryservice.config.converters;

import com.cryptocraft.inventoryservice.dao.enums.Type;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;


@Converter(autoApply = true)
public class TypeConverter implements AttributeConverter<Type, Short> {

    @Override
    public Short convertToDatabaseColumn(Type attribute) {
        return attribute == null ? null : attribute.id().shortValue();
    }

    @Override
    public Type convertToEntityAttribute(Short dbData) {
        return dbData == null ? null : Type.byId(dbData.intValue());
    }
}
