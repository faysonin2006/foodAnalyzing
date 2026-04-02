package com.userservice.common.exceptions;

public class MealEntryNotFoundException extends RuntimeException {
    public MealEntryNotFoundException(String message) {
        super(message);
    }
}
