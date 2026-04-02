package com.userservice.pantry.service.openfoodfacts.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenFoodFactsProductDto {

    private String code;

    @JsonProperty("product_name")
    private String productName;

    private String brands;

    private String categories;

    @JsonProperty("categories_tags")
    private List<String> categoriesTags;

    @JsonProperty("image_front_url")
    private String imageFrontUrl;

    @JsonProperty("expiration_date")
    private String expirationDate;

    @JsonProperty("product_quantity")
    private BigDecimal productQuantity;

    @JsonProperty("product_quantity_unit")
    private String productQuantityUnit;

    private String quantity;
}
