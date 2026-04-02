package com.recipeservice.dtos.internal.pantry;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PantryListItemResponse {
    private UUID id;
    private String name;
    private String brand;
    private String category;
    private BigDecimal quantity;
    private String unit;
    private LocalDate expiresAt;
    private String status;
    private String imageUrl;
}
