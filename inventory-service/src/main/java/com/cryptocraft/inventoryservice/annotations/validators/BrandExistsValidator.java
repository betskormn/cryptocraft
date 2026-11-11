package com.cryptocraft.inventoryservice.annotations.validators;

import com.cryptocraft.inventoryservice.annotations.BrandExists;
import com.cryptocraft.inventoryservice.repositories.BrandRepository;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class BrandExistsValidator implements ConstraintValidator<BrandExists, Long> {

    private final BrandRepository brandRepository;

    public BrandExistsValidator(BrandRepository brandRepository) {
        this.brandRepository = brandRepository;
    }

    @Override
    public void initialize(BrandExists constraintAnnotation) {
    }

    @Override
    public boolean isValid(Long brandId, ConstraintValidatorContext context) {
        if (brandId == null) {
            return false;
        }
        boolean brandExists = brandRepository.findById(brandId)
                .isPresent();
        if (!brandExists) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(String.format("Brand with id:%s, does not exist", brandId))
                    .addConstraintViolation();
            log.info(String.format(String.format("Validation failed. Brand with id:%s, does not exist", brandId)));
        }
        return brandExists;
    }

}
