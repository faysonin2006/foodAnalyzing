package com.authservice.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserAuthResponse {
    private String accessToken;
    private String refreshToken;
    private Long expiresIn;
}
