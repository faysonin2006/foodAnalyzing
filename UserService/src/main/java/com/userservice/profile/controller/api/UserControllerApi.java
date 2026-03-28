package com.userservice.profile.controller.api;

import com.userservice.common.exceptions.ErrorResponse;
import com.userservice.profile.dto.CreateProfileRequest;
import com.userservice.profile.dto.UserProfileResponse;
import com.userservice.profile.dto.UserProfileUpdateRequest;
import com.userservice.profile.dto.likes.LikeActionResponse;
import com.userservice.profile.dto.likes.LikedRecipeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Tag(name = "Profiles", description = "Profile management and recipe likes")
public interface UserControllerApi {

    @Operation(summary = "Create recipe like", description = "Creates a like for the current authenticated user.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Like created"),
            @ApiResponse(responseCode = "200", description = "Like already existed"),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Profile not found")
    })
    ResponseEntity<LikeActionResponse> createLike(
            @Parameter(description = "Recipe identifier", example = "42") @PathVariable Long recipeId
    );

    @Operation(summary = "Remove recipe like", description = "Removes a like for the current authenticated user.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Like removed"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    ResponseEntity<LikeActionResponse> removeLike(
            @Parameter(description = "Recipe identifier", example = "42") @PathVariable Long recipeId
    );

    @Operation(summary = "List likes", description = "Returns liked recipe ids for the current authenticated user.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Likes returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    ResponseEntity<List<LikedRecipeResponse>> getLikes();

    @Operation(summary = "Create profile", description = "Creates a profile for a user coming from AuthService.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Profile created"),
            @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<Void> createProfile(@Valid @RequestBody CreateProfileRequest request);

    @Operation(summary = "Get current profile", description = "Returns profile details for the authenticated user.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Profile not found")
    })
    ResponseEntity<UserProfileResponse> getMyProfile();

    @Operation(summary = "Update current profile", description = "Updates mutable profile fields for the authenticated user.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile updated"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Profile not found")
    })
    ResponseEntity<UserProfileResponse> updateMyProfile(@Valid @RequestBody UserProfileUpdateRequest request);

    @Operation(summary = "Get internal profile by email", description = "Internal endpoint for other services to resolve profile data by email.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile returned"),
            @ApiResponse(responseCode = "404", description = "Profile not found")
    })
    ResponseEntity<UserProfileResponse> getUserProfileById(
            @Parameter(description = "User email", example = "user@example.com") @PathVariable String email
    );
}
