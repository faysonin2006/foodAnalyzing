package com.authservice.models.enums;

import lombok.Getter;

@Getter
public enum Role {

    USER("Regular user with standard access"),
    ADMIN("Administrator with full system access"),
    MODERATOR("Content moderator with limited admin privileges");

    private final String description;

    Role(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
