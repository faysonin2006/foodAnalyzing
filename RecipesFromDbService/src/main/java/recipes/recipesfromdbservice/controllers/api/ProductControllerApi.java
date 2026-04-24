package recipes.recipesfromdbservice.controllers.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import recipes.recipesfromdbservice.configs.exceptionhandler.ErrorResponse;
import recipes.recipesfromdbservice.dtos.ProductCardResponse;
import recipes.recipesfromdbservice.dtos.ProductDetailResponse;
import recipes.recipesfromdbservice.dtos.ProductSearchPageResponse;

import java.util.List;

@Tag(name = "Products DB", description = "Packaged food product search and details backed by the local product catalog")
public interface ProductControllerApi {

    @Operation(
            summary = "Search product catalog",
            description = "Returns packaged products with calories and macros per 100g/100ml from the local product database.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Products returned"),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<List<ProductCardResponse>> searchProducts(
            @Parameter(description = "Free-text product query", example = "milk", required = true) String q,
            @Parameter(description = "Optional country filter", example = "russia") String country,
            @Parameter(description = "Page number", example = "1") Integer page,
            @Parameter(description = "Page size", example = "20") Integer size
    );

    @Operation(
            summary = "Search product catalog with paging",
            description = "Returns paged packaged product results optimized for large catalog browsing screens.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product page returned"),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<ProductSearchPageResponse> searchProductsPage(
            @Parameter(description = "Free-text product query", example = "milk", required = true) String q,
            @Parameter(description = "Optional country filter", example = "russia") String country,
            @Parameter(description = "Page number", example = "1") Integer page,
            @Parameter(description = "Page size", example = "20") Integer size
    );

    @Operation(
            summary = "Get product details by barcode/code",
            description = "Returns one packaged product with nutrition values per 100g/100ml.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product returned"),
            @ApiResponse(responseCode = "400", description = "Invalid code",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Product not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<ProductDetailResponse> getProductByCode(
            @Parameter(description = "Open Food Facts product code / barcode", example = "0054881005906", required = true)
            String code
    );

    @Operation(
            summary = "Get product details by barcode",
            description = "Returns one packaged product by exact barcode / code match.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product returned"),
            @ApiResponse(responseCode = "400", description = "Invalid barcode",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Product not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<ProductDetailResponse> getProductByBarcode(
            @Parameter(description = "Barcode / product code", example = "0054881005906", required = true)
            String barcode
    );
}
