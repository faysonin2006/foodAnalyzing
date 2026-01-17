package com.userservice.controllers;

import com.userservice.dtos.CreateProfileRequest;
import com.userservice.dtos.UserProfileResponse;
import com.userservice.dtos.UserProfileUpdateRequest;
import com.userservice.services.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profiles")
@RequiredArgsConstructor
public class UserController {

    private final UserProfileService userProfileService;

    @PostMapping
    public ResponseEntity<Void> createProfile(@RequestBody @Valid CreateProfileRequest request) {
        userProfileService.createProfile(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMyProfile(Authentication authentication) {
        String email = authentication.getName();
        UserProfileResponse response = userProfileService.getProfileByEmail(email);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfileResponse> updateMyProfile(Authentication authentication, @RequestBody @Valid UserProfileUpdateRequest request) {
        String email = authentication.getName();
        UserProfileResponse response = userProfileService.updateProfile(email, request);
        return ResponseEntity.ok(response);
    }
    @GetMapping("/internal/{email}")
    public ResponseEntity<UserProfileResponse> getUserProfileById(@PathVariable String email) {
        return ResponseEntity.ok(userProfileService.getProfileByEmail(email));
    }
}
