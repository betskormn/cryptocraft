package com.cryptocraft.inventoryservice.exceptions;


import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ExceptionHandlerMessageUtil {
    private List<String> composeBindingResultMessage(MethodArgumentNotValidException ex) {
        return ex.getBindingResult()
                .getAllErrors().stream().map(DefaultMessageSourceResolvable::getDefaultMessage).collect(Collectors.toList());
    }

    private List<String> composeThrowableMessage(Throwable ex) {
        return Stream.of(ex.getMessage())
                .map(String::toString).collect(Collectors.toList());
    }

    public List<String> composeMessage(Throwable ex) {
        if (ex instanceof MethodArgumentNotValidException) {
            return composeBindingResultMessage((MethodArgumentNotValidException) ex);
        } else {
            return composeThrowableMessage(ex);
        }
    }
}
