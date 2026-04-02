package com.userservice.pantry.dto;

import com.userservice.pantry.model.enums.PantryUnit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PantryBarcodeLookupResponse {

    private String barcode;
    private String name;
    private String brand;
    private String category;
    private String imageUrl;
    private BigDecimal suggestedQuantity;
    private PantryUnit suggestedUnit;
    private String rawQuantity;
    private LocalDate expiresAt;
    private String source;
    private Map<String, String> fieldSources;
}
