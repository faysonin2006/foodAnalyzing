package com.recipeservice.clients;

import com.recipeservice.dtos.internal.shopping.CreateShoppingListItemRequest;
import com.recipeservice.dtos.internal.shopping.ShoppingListItemResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.List;

@HttpExchange("/api/shopping-lists/internal")
public interface UserShoppingListClient {

    @PostExchange("/{email}/items")
    List<ShoppingListItemResponse> createItems(@PathVariable String email, @RequestBody List<CreateShoppingListItemRequest> requests);
}
