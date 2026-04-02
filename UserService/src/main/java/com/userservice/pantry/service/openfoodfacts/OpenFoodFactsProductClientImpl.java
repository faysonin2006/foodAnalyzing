package com.userservice.pantry.service.openfoodfacts;

import com.userservice.common.constants.AppMessages;
import com.userservice.common.exceptions.ProductLookupNotFoundException;
import com.userservice.common.exceptions.UpstreamServiceException;
import com.userservice.pantry.service.OpenFoodFactsProductClient;
import com.userservice.pantry.service.openfoodfacts.dto.OpenFoodFactsProductLookupResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class OpenFoodFactsProductClientImpl implements OpenFoodFactsProductClient {

    private final RestClient restClient;
    private final String locale;

    public OpenFoodFactsProductClientImpl(
            RestClient.Builder restClientBuilder,
            @Value("${pantry.barcode.lookup.base-url:https://world.openfoodfacts.org}") String baseUrl,
            @Value("${pantry.barcode.lookup.user-agent:FoodAnalyzing/1.0}") String userAgent,
            @Value("${pantry.barcode.lookup.locale:en}") String locale
    ) {
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader("User-Agent", userAgent)
                .build();
        this.locale = locale;
    }

    @Override
    public OpenFoodFactsProductLookupResponse lookupProductByBarcode(String barcode) {
        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v2/product/{barcode}.json")
                            .queryParam("fields", "code,product_name,brands,categories,categories_tags,image_front_url,product_quantity,product_quantity_unit,quantity,expiration_date")
                    .queryParam("lc", locale)
                    .build(barcode))
                    .retrieve()
                    .body(OpenFoodFactsProductLookupResponse.class);
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 404) {
                throw new ProductLookupNotFoundException(AppMessages.PRODUCT_NOT_FOUND_BY_BARCODE);
            }
            throw new UpstreamServiceException(AppMessages.FAILED_TO_LOOKUP_PRODUCT_BY_BARCODE, exception);
        } catch (Exception exception) {
            throw new UpstreamServiceException(AppMessages.FAILED_TO_LOOKUP_PRODUCT_BY_BARCODE, exception);
        }
    }
}
