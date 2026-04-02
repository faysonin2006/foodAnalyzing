package com.userservice;

import com.userservice.common.exceptions.BadRequestException;
import com.userservice.common.exceptions.ProductLookupNotFoundException;
import com.userservice.pantry.dto.PantryBarcodeLookupResponse;
import com.userservice.pantry.model.enums.PantryUnit;
import com.userservice.pantry.service.OpenFoodFactsProductClient;
import com.userservice.pantry.service.PantryBarcodeLookupService;
import com.userservice.pantry.service.openfoodfacts.dto.OpenFoodFactsProductDto;
import com.userservice.pantry.service.openfoodfacts.dto.OpenFoodFactsProductLookupResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PantryBarcodeLookupServiceTest {

    @Mock
    private OpenFoodFactsProductClient openFoodFactsProductClient;

    @Test
    void lookupByBarcodeShouldMapOpenFoodFactsProduct() {
        PantryBarcodeLookupService service = new PantryBarcodeLookupService(openFoodFactsProductClient);
        when(openFoodFactsProductClient.lookupProductByBarcode("5449000000996")).thenReturn(
                lookupResponse(
                        "5449000000996",
                        "Coca-Cola Original Taste",
                        "Coca-Cola",
                        "Beverages and beverages preparations,Beverages",
                        "https://images.openfoodfacts.org/front.jpg",
                        null,
                        null,
                        "330 ml"
                )
        );

        PantryBarcodeLookupResponse response = service.lookupByBarcode("5449000000996");

        assertEquals("5449000000996", response.getBarcode());
        assertEquals("Coca-Cola Original Taste", response.getName());
        assertEquals("Coca-Cola", response.getBrand());
        assertEquals("Beverages and beverages preparations", response.getCategory());
        assertEquals(new BigDecimal("330"), response.getSuggestedQuantity());
        assertEquals(PantryUnit.MILLILITER, response.getSuggestedUnit());
        assertEquals("OPEN_FOOD_FACTS", response.getSource());
    }

    @Test
    void lookupByBarcodeShouldRejectInvalidBarcode() {
        PantryBarcodeLookupService service = new PantryBarcodeLookupService(openFoodFactsProductClient);

        assertThrows(BadRequestException.class, () -> service.lookupByBarcode("abc"));
    }

    @Test
    void lookupByBarcodeShouldThrowWhenProductMissing() {
        PantryBarcodeLookupService service = new PantryBarcodeLookupService(openFoodFactsProductClient);
        OpenFoodFactsProductLookupResponse lookupResponse = new OpenFoodFactsProductLookupResponse();
        lookupResponse.setStatus(0);
        when(openFoodFactsProductClient.lookupProductByBarcode("12345678")).thenReturn(lookupResponse);

        assertThrows(ProductLookupNotFoundException.class, () -> service.lookupByBarcode("12345678"));
    }

    private OpenFoodFactsProductLookupResponse lookupResponse(
            String code,
            String productName,
            String brand,
            String categories,
            String imageUrl,
            BigDecimal quantity,
            String quantityUnit,
            String rawQuantity
    ) {
        OpenFoodFactsProductDto product = new OpenFoodFactsProductDto();
        product.setCode(code);
        product.setProductName(productName);
        product.setBrands(brand);
        product.setCategories(categories);
        product.setImageFrontUrl(imageUrl);
        product.setProductQuantity(quantity);
        product.setProductQuantityUnit(quantityUnit);
        product.setQuantity(rawQuantity);

        OpenFoodFactsProductLookupResponse response = new OpenFoodFactsProductLookupResponse();
        response.setCode(code);
        response.setStatus(1);
        response.setProduct(product);
        return response;
    }
}
