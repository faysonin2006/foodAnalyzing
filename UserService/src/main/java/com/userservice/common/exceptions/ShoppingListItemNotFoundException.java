package com.userservice.common.exceptions;

public class ShoppingListItemNotFoundException extends RuntimeException {
    public ShoppingListItemNotFoundException(String message) {
        super(message);
    }
}
