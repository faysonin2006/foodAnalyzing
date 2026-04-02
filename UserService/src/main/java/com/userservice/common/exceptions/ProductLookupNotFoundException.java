package com.userservice.common.exceptions;

public class ProductLookupNotFoundException extends RuntimeException {
    public ProductLookupNotFoundException(String message) {
        super(message);
    }
}
