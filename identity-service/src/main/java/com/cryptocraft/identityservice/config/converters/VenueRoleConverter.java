package com.cryptocraft.identityservice.config.converters;

import com.cryptocraft.security.VenueRole;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;


@Converter(autoApply = true)
public class VenueRoleConverter implements AttributeConverter<VenueRole, Short> {

    @Override
    public Short convertToDatabaseColumn(VenueRole attribute) {
        return attribute == null ? null : attribute.id().shortValue();
    }

    @Override
    public VenueRole convertToEntityAttribute(Short dbData) {
        return dbData == null ? null : VenueRole.byId(dbData.intValue());
    }
}
