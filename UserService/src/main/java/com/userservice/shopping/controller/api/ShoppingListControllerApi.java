package com.userservice.shopping.controller.api;

import com.userservice.common.exceptions.ErrorResponse;
import com.userservice.shopping.dto.CreateShoppingListItemRequest;
import com.userservice.shopping.dto.ShoppingListItemResponse;
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
import java.util.UUID;

@Tag(name = "Shopping List", description = "Shopping list management")
public interface ShoppingListControllerApi {

    @Operation(summary = "Create shopping list item", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Shopping list item created"),
            @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    ResponseEntity<ShoppingListItemResponse> createItem(@Valid @RequestBody CreateShoppingListItemRequest request);

    @Operation(summary = "Get shopping list items", security = @SecurityRequirement(name = "bearerAuth"))
    ResponseEntity<List<ShoppingListItemResponse>> getItems();

    @Operation(summary = "Toggle checked flag", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Shopping list item updated"),
            @ApiResponse(responseCode = "404", description = "Shopping list item not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<ShoppingListItemResponse> toggleChecked(
            @Parameter(example = "11111111-1111-1111-1111-111111111111") @PathVariable UUID itemId
    );

    @Operation(summary = "Delete shopping list item", security = @SecurityRequirement(name = "bearerAuth"))
    ResponseEntity<Void> deleteItem(
            @Parameter(example = "11111111-1111-1111-1111-111111111111") @PathVariable UUID itemId
    );
}
