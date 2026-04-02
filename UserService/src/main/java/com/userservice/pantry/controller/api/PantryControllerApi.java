package com.userservice.pantry.controller.api;

import com.userservice.common.exceptions.ErrorResponse;
import com.userservice.pantry.dto.CreatePantryItemRequest;
import com.userservice.pantry.dto.PantryBarcodeLookupResponse;
import com.userservice.pantry.dto.PantryItemResponse;
import com.userservice.pantry.dto.PantryListItemResponse;
import com.userservice.pantry.dto.UpdatePantryItemRequest;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Tag(name = "Pantry", description = "User pantry items management")
public interface PantryControllerApi {

    @Operation(
            summary = "Create pantry item",
            description = "Creates a pantry item for the authenticated user.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pantry item created"),
            @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Profile not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<PantryItemResponse> createPantryItem(@Valid @RequestBody CreatePantryItemRequest request);

    @Operation(
            summary = "Get pantry items",
            description = "Returns pantry items for the authenticated user.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pantry items returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<List<PantryListItemResponse>> getPantryItems();

    @Operation(
            summary = "Lookup pantry product by barcode",
            description = "Returns product prefill data for pantry creation based on a scanned barcode. Camera scanning is expected on the client side.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product lookup returned"),
            @ApiResponse(responseCode = "400", description = "Barcode is invalid", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Product not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "502", description = "External product catalog lookup failed", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<PantryBarcodeLookupResponse> lookupProductByBarcode(
            @Parameter(description = "EAN/UPC barcode", example = "5449000000996")
            @PathVariable String barcode
    );

    @Operation(
            summary = "Get pantry item by id",
            description = "Returns one pantry item for the authenticated user.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pantry item returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Pantry item not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<PantryItemResponse> getPantryItemById(
            @Parameter(description = "Pantry item identifier", example = "11111111-1111-1111-1111-111111111111")
            @PathVariable UUID pantryItemId
    );

    @Operation(
            summary = "Update pantry item",
            description = "Updates a pantry item for the authenticated user.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pantry item updated"),
            @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Pantry item not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<PantryItemResponse> updatePantryItem(
            @Parameter(description = "Pantry item identifier", example = "11111111-1111-1111-1111-111111111111")
            @PathVariable UUID pantryItemId,
            @Valid @RequestBody UpdatePantryItemRequest request
    );

    @Operation(
            summary = "Upload pantry item image",
            description = "Uploads an image for a pantry item and stores it in S3-compatible storage.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pantry item image uploaded"),
            @ApiResponse(responseCode = "400", description = "Invalid file or pantry item request", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Pantry item not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Storage error", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<PantryItemResponse> uploadPantryItemImage(
            @Parameter(description = "Pantry item identifier", example = "11111111-1111-1111-1111-111111111111")
            @PathVariable UUID pantryItemId,
            @Parameter(description = "Image file to upload")
            @RequestParam("file") MultipartFile file
    );

    @Operation(
            summary = "Delete pantry item",
            description = "Soft deletes a pantry item for the authenticated user.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Pantry item deleted"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Pantry item not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<Void> deletePantryItem(
            @Parameter(description = "Pantry item identifier", example = "11111111-1111-1111-1111-111111111111")
            @PathVariable UUID pantryItemId
    );

    @Operation(
            summary = "Get expiring soon items",
            description = "Returns active pantry items that will expire soon.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Expiring pantry items returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<List<PantryListItemResponse>> getExpiringSoonItems();

    @Operation(
            summary = "Get expired items",
            description = "Returns expired pantry items for the authenticated user.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Expired pantry items returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<List<PantryListItemResponse>> getExpiredItems();
}
