package com.authservice.controllers.api;

import com.authservice.config.exceptionhandlers.model.ErrorResponse;
import com.authservice.dtos.UserAuthResponse;
import com.authservice.dtos.UserLoginRequest;
import com.authservice.dtos.UserRegistrationRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@Tag(name = "Authentication", description = "Registration, login and refresh token flow")
public interface AuthControllerApi {

    @Operation(summary = "Register user", description = "Creates a new user account and returns issued tokens.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User registered"),
            @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Email already exists", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<UserAuthResponse> register(@Valid @RequestBody UserRegistrationRequest userRegistrationRequest);

    @Operation(summary = "Login user", description = "Authenticates user credentials and returns new tokens.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<UserAuthResponse> login(@Valid @RequestBody UserLoginRequest request);

    @Operation(summary = "Refresh access token", description = "Reissues an access token for a valid refresh token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token refreshed"),
            @ApiResponse(responseCode = "401", description = "Refresh token invalid or expired", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<UserAuthResponse> refresh(
            @Parameter(description = "Refresh token", required = true) @RequestHeader("Refresh-Token") String refreshToken
    );
}
