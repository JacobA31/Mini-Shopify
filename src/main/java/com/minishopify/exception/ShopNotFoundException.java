package com.minishopify.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Thrown when a shop id doesn't exist. Spring turns it into a 404 (templates/error/404.html). */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ShopNotFoundException extends RuntimeException {

    public ShopNotFoundException(Long shopId) {
        super("Shop " + shopId + " not found");
    }
}