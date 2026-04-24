package recipes.recipesfromdbservice.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import recipes.recipesfromdbservice.controllers.api.ProductControllerApi;
import recipes.recipesfromdbservice.dtos.ProductCardResponse;
import recipes.recipesfromdbservice.dtos.ProductDetailResponse;
import recipes.recipesfromdbservice.dtos.ProductSearchPageResponse;
import recipes.recipesfromdbservice.services.ProductService;

import java.util.List;

@RestController
@RequestMapping("/api/recipes/db/products")
@RequiredArgsConstructor
public class ProductController implements ProductControllerApi {

    private final ProductService productService;

    @Override
    @GetMapping("/search")
    public ResponseEntity<List<ProductCardResponse>> searchProducts(
            @RequestParam("q") String q,
            @RequestParam(value = "country", required = false) String country,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size
    ) {
        return ResponseEntity.ok(productService.searchProducts(q, country, page, size));
    }

    @Override
    @GetMapping("/search-page")
    public ResponseEntity<ProductSearchPageResponse> searchProductsPage(
            @RequestParam("q") String q,
            @RequestParam(value = "country", required = false) String country,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size
    ) {
        return ResponseEntity.ok(productService.searchProductsPage(q, country, page, size));
    }

    @Override
    @GetMapping("/{code}")
    public ResponseEntity<ProductDetailResponse> getProductByCode(@PathVariable String code) {
        return ResponseEntity.ok(productService.getProductByCode(code));
    }

    @Override
    @GetMapping("/barcode/{barcode}")
    public ResponseEntity<ProductDetailResponse> getProductByBarcode(@PathVariable String barcode) {
        return ResponseEntity.ok(productService.getProductByBarcode(barcode));
    }
}
