package com.aiimageservice.controllers.api;

import com.aiimageservice.dtos.FoodAnalysisDetailResponse;
import com.aiimageservice.dtos.FoodAnalysisResponse;
import com.aiimageservice.dtos.SaveFoodAnalysisRequest;
import com.aiimageservice.dtos.SaveFoodAnalysisResponse;
import com.aiimageservice.exceptions.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Tag(name = "Food analysis", description = "Upload food images and fetch analysis history")
public interface FoodAnalysisControllerApi {

    @Operation(summary = "Start food analysis", description = "Uploads an image and schedules AI processing.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Analysis scheduled"),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    ResponseEntity<FoodAnalysisResponse> analyzeFood(
            @Parameter(description = "Image file", required = true) @RequestParam("file") MultipartFile file,
            @Parameter(description = "Optional extra questions for the AI model", required = false, example = "Can I eat this while losing weight?") @RequestParam(value = "extraQuestions", required = false) String questions
    );

    @Operation(summary = "Get analysis by id", description = "Returns detailed information for a specific food analysis.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Analysis found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Analysis not found")
    })
    ResponseEntity<FoodAnalysisDetailResponse> getAnalysis(
            @Parameter(description = "Analysis identifier", example = "54b35d30-1d29-4d6b-9850-39d0fbbddc2e") @PathVariable UUID id
    );

    @Operation(summary = "Save analysis as meal", description = "Confirms and saves completed AI analysis as a meal history entry.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Meal saved from analysis"),
            @ApiResponse(responseCode = "400", description = "Analysis is not ready or request is invalid", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Analysis not found"),
            @ApiResponse(responseCode = "500", description = "Meal save failed", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<SaveFoodAnalysisResponse> saveAnalysis(
            @Parameter(description = "Analysis identifier", example = "54b35d30-1d29-4d6b-9850-39d0fbbddc2e") @PathVariable UUID id,
            @Valid @RequestBody(required = false) SaveFoodAnalysisRequest request
    );

    @Operation(summary = "Get analysis history", description = "Returns analyses for the current authenticated user.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "History returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    ResponseEntity<List<FoodAnalysisResponse>> getHistory();

    @Operation(summary = "Delete analysis from history", description = "Deletes one analysis record for the current authenticated user.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "History item deleted"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Analysis not found")
    })
    @DeleteMapping("/history/{id}")
    ResponseEntity<Void> deleteHistoryItem(
            @Parameter(description = "Analysis identifier", example = "54b35d30-1d29-4d6b-9850-39d0fbbddc2e") @PathVariable UUID id
    );
}
