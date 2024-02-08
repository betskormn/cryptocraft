package com.cryptocraft.inventoryservice.annotations;

import com.cryptocraft.inventoryservice.annotations.validators.BrandExistsValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = BrandExistsValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface BrandExists {

    String message() default "Brand does not exist";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
