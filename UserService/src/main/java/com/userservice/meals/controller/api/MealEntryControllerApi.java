package com.userservice.meals.controller.api;

import com.userservice.common.exceptions.ErrorResponse;
import com.userservice.meals.dto.CreateMealEntryRequest;
import com.userservice.meals.dto.MealEntryResponse;
import com.userservice.meals.dto.MealListItemResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Tag(name = "Meals", description = "Meal history management")
public interface MealEntryControllerApi {

    @Operation(summary = "Create meal entry", description = "Creates a meal entry for the authenticated user.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Meal entry created"),
            @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Profile not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<MealEntryResponse> createMeal(@Valid @RequestBody CreateMealEntryRequest request);

    @Operation(summary = "Get meals", description = "Returns meal history for the authenticated user. Supports optional date range filtering.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Meal entries returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<List<MealListItemResponse>> getMeals(
            @Parameter(description = "Start date filter", example = "2026-03-01")
            @RequestParam(value = "dateFrom", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @Parameter(description = "End date filter", example = "2026-03-31")
            @RequestParam(value = "dateTo", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo
    );

    @Operation(summary = "Get meal entry by id", description = "Returns one meal entry for the authenticated user.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Meal entry returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Meal entry not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<MealEntryResponse> getMealById(
            @Parameter(description = "Meal entry identifier", example = "11111111-1111-1111-1111-111111111111")
            @PathVariable UUID mealEntryId
    );

    @Operation(summary = "Delete meal entry", description = "Deletes a meal entry for the authenticated user.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Meal entry deleted"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Meal entry not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<Void> deleteMeal(
            @Parameter(description = "Meal entry identifier", example = "11111111-1111-1111-1111-111111111111")
            @PathVariable UUID mealEntryId
    );
}
