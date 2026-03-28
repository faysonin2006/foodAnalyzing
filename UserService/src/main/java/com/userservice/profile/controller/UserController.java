package com.userservice.profile.controller;

import com.userservice.profile.controller.api.UserControllerApi;
import com.userservice.profile.dto.CreateProfileRequest;
import com.userservice.profile.dto.UserProfileResponse;
import com.userservice.profile.dto.UserProfileUpdateRequest;
import com.userservice.profile.dto.likes.LikeActionResponse;
import com.userservice.profile.dto.likes.LikedRecipeResponse;
import com.userservice.profile.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/profiles")
@RequiredArgsConstructor
public class UserController implements UserControllerApi {

    private final UserService userService;

    @Override
    @PostMapping("/likes/{recipeId}")
    public ResponseEntity<LikeActionResponse> createLike(@PathVariable Long recipeId) {
        LikeActionResponse response = userService.createLike(recipeId);
        return ResponseEntity.status(response.isChanged() ? HttpStatus.CREATED : HttpStatus.OK).body(response);
    }

    @Override
    @DeleteMapping("/likes/{recipeId}")
    public ResponseEntity<LikeActionResponse> removeLike(@PathVariable Long recipeId) {
        return ResponseEntity.ok(userService.removeLike(recipeId));
    }

    @Override
    @GetMapping("/likes")
    public ResponseEntity<List<LikedRecipeResponse>> getLikes() {
        return ResponseEntity.ok(userService.getAllLikes());
    }

    @Override
    @PostMapping
    public ResponseEntity<Void> createProfile(@RequestBody @Valid CreateProfileRequest request) {
        userService.createProfile(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Override
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMyProfile() {
        return ResponseEntity.ok(userService.getCurrentProfile());
    }

    @Override
    @PutMapping("/me")
    public ResponseEntity<UserProfileResponse> updateMyProfile(@RequestBody @Valid UserProfileUpdateRequest request) {
        return ResponseEntity.ok(userService.updateCurrentProfile(request));
    }

    @Override
    @GetMapping("/internal/{email}")
    public ResponseEntity<UserProfileResponse> getUserProfileById(@PathVariable String email) {
        return ResponseEntity.ok(userService.getProfileByEmail(email));
    }
}
