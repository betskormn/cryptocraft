package com.cryptocraft.inventoryservice.exceptions;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.client.HttpServerErrorException;

import java.nio.file.AccessDeniedException;
import java.util.Arrays;

public enum ExceptionGlossary {
    ACCESS_DENIED(AccessDeniedException.class, HttpStatus.FORBIDDEN),
    ILLEGAL_ARGUMENT(IllegalArgumentException.class, HttpStatus.BAD_REQUEST),
    //    PRECONDITION_FAILED(PromotionOfferProcessingException.class, HttpStatus.PRECONDITION_FAILED),
//    INVALID_CREDENTIALS(BadCredentialsException.class, UNAUTHORIZED),
    INTERNAL_SERVER_ERROR(HttpServerErrorException.class, HttpStatus.INTERNAL_SERVER_ERROR),
    //    FORBIDDEN(ConsumerEmailConfirmationException.class, HttpStatus.PRECONDITION_FAILED),
//    BAD_REQUEST(ConsumerTokenException.class, HttpStatus.BAD_REQUEST),
//    REDEMPTION_FAILED(ConsumerRedemptionException.class, HttpStatus.PRECONDITION_FAILED),
//    NOT_FOUND(ResourceNotFoundException.class, HttpStatus.NOT_FOUND),
//    DUPLICATE_ERROR(DuplicateEntityConstraintException.class, HttpStatus.CONFLICT),
//    ACCOUNT_STATUS_ERROR(InvalidAccountStatusException.class, CONFLICT),
    NOT_VALID_VARIABLE(MethodArgumentNotValidException.class, HttpStatus.BAD_REQUEST),
    TENANT_ALREADY_EXISTS(TenantAlreadyExistsException.class, HttpStatus.CONFLICT),
//    CONSUMER_TOO_FAR_TO_START_REDEMPTION(ConsumerTooFarToRedeemException.class, HttpStatus.FORBIDDEN),
    ENTITY_NOT_FOUND(EntityNotFoundException .class, HttpStatus.NOT_FOUND);
//    UNSUPPORTED_VERSION(UnsupportedVersionException.class, HttpStatus.NOT_ACCEPTABLE);

    public final Class<? extends Throwable> aClass;
    public final HttpStatus code;

    ExceptionGlossary(Class<? extends Throwable> aClass, HttpStatus code) {
        this.aClass = aClass;
        this.code = code;
    }

    public static HttpStatus getCodeByException(Class<? extends Throwable> aClass) {
        ExceptionGlossary exception = Arrays.stream(ExceptionGlossary.values()).filter(it -> it.aClass.equals(aClass)).findFirst()
                .orElse(INTERNAL_SERVER_ERROR);
        return exception.code;
    }
}