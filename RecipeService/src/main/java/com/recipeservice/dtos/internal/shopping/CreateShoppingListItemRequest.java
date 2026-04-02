package com.recipeservice.dtos.internal.shopping;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateShoppingListItemRequest {

    private String name;
    private BigDecimal quantity;
    private String unit;
}
