package com.userservice.common.exceptions;

public class PantryItemNotFoundException extends RuntimeException {
    public PantryItemNotFoundException(String message) {
        super(message);
    }
}
