package com.recipeservice.dtos.recommendations;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddMissingIngredientsResponse {

    private Long recipeId;
    private String recipeTitle;
    private int addedItemsCount;
    private List<AddedShoppingListItem> items;
}
