package com.authservice;

import com.authservice.dtos.UserLoginRequest;
import com.authservice.dtos.UserRegistrationRequest;
import com.authservice.models.RefreshToken;
import com.authservice.models.UserCredentials;
import com.authservice.models.enums.Role;
import lombok.experimental.UtilityClass;

import java.time.Instant;
import java.util.UUID;

@UtilityClass
public class TestDataFactory {

    public static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    public static UserRegistrationRequest registrationRequest() {
        return new UserRegistrationRequest("user@example.com", "password123", null, null);
    }

    public static UserLoginRequest loginRequest() {
        return new UserLoginRequest("user@example.com", "password123");
    }

    public static UserCredentials user() {
        return UserCredentials.builder()
                .id(USER_ID)
                .email("user@example.com")
                .passwordHash("encoded")
                .role(Role.USER)
                .build();
    }

    public static RefreshToken refreshToken(UserCredentials user, Instant expiryDate) {
        return RefreshToken.builder()
                .id(1L)
                .token("refresh-token")
                .expiryDate(expiryDate)
                .user(user)
                .build();
    }
}
