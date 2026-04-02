package com.recipeservice.dtos.internal.shopping;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShoppingListItemResponse {

    private UUID id;
    private String name;
    private BigDecimal quantity;
    private String unit;
    private boolean checked;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
