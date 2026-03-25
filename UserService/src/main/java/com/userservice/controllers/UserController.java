package com.userservice.controllers;

import com.userservice.dtos.CreateProfileRequest;
import com.userservice.dtos.UserProfileResponse;
import com.userservice.dtos.UserProfileUpdateRequest;
import com.userservice.dtos.likes.LikeActionResponse;
import com.userservice.dtos.likes.LikedRecipeResponse;
import com.userservice.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/profiles")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;


    @PostMapping("/likes/{recipeId}")
    public ResponseEntity<LikeActionResponse> createLike(
            Authentication authentication,
            @PathVariable Long recipeId
    ) {
        LikeActionResponse response = userService.createLike(authentication.getName(), recipeId);
        return ResponseEntity.status(response.isChanged() ? HttpStatus.CREATED : HttpStatus.OK).body(response);
    }

    @DeleteMapping("/likes/{recipeId}")
    public ResponseEntity<LikeActionResponse> removeLike(
            Authentication authentication,
            @PathVariable Long recipeId
    ) {
        return ResponseEntity.ok(userService.removeLike(authentication.getName(), recipeId));
    }

    @GetMapping("/likes")
    public ResponseEntity<List<LikedRecipeResponse>> getLikes(Authentication authentication) {
        return ResponseEntity.ok(userService.getAllLikes(authentication.getName()));
    }

    @PostMapping
    public ResponseEntity<Void> createProfile(@RequestBody @Valid CreateProfileRequest request) {
        userService.createProfile(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMyProfile(Authentication authentication) {
        String email = authentication.getName();
        UserProfileResponse response = userService.getProfileByEmail(email);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfileResponse> updateMyProfile(Authentication authentication, @RequestBody @Valid UserProfileUpdateRequest request) {
        String email = authentication.getName();
        UserProfileResponse response = userService.updateProfile(email, request);
        return ResponseEntity.ok(response);
    }
    @GetMapping("/internal/{email}")
    public ResponseEntity<UserProfileResponse> getUserProfileById(@PathVariable String email) {
        return ResponseEntity.ok(userService.getProfileByEmail(email));
    }
}
