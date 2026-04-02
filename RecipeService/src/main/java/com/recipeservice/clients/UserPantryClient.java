package com.recipeservice.clients;

import com.recipeservice.dtos.internal.pantry.PantryListItemResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.List;

@HttpExchange("/api/pantry/internal")
public interface UserPantryClient {

    @GetExchange("/{email}")
    List<PantryListItemResponse> getPantry(@PathVariable String email);
}
