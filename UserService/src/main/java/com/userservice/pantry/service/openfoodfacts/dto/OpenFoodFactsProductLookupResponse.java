package com.userservice.pantry.service.openfoodfacts.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenFoodFactsProductLookupResponse {

    private String code;
    private Integer status;

    @JsonProperty("status_verbose")
    private String statusVerbose;

    private OpenFoodFactsProductDto product;
}
