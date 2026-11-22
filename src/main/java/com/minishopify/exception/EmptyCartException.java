package com.minishopify.exception;

/** Thrown when a customer tries to check out without any items. */
public class EmptyCartException extends RuntimeException {

    public EmptyCartException() {
        super("Your cart is empty.");
    }

}
