package com.userservice.pantry.service;

import com.userservice.pantry.service.openfoodfacts.dto.OpenFoodFactsProductLookupResponse;

public interface OpenFoodFactsProductClient {

    OpenFoodFactsProductLookupResponse lookupProductByBarcode(String barcode);
}
