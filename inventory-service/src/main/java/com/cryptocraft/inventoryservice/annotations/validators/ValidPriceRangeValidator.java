package com.cryptocraft.inventoryservice.annotations.validators;

import com.cryptocraft.inventoryservice.annotations.ValidPriceRange;
import com.cryptocraft.inventoryservice.controllers.dto.request.ItemRequestDto;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ValidPriceRangeValidator implements ConstraintValidator<ValidPriceRange, ItemRequestDto> {

    @Override
    public boolean isValid(ItemRequestDto dto, ConstraintValidatorContext context) {

        if (dto == null) {
            return true;
        }
        BigDecimal min = dto.minPrice();
        BigDecimal base = dto.basePrice();
        BigDecimal max = dto.maxPrice();

        if (min == null || base == null || max == null) {
            return true;
        }
        boolean valid = min.compareTo(base) <= 0 && base.compareTo(max) <= 0;

        if (!valid) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "Price constraint violated: min <= base <= max"
            ).addConstraintViolation();
        }
        return valid;
    }
}