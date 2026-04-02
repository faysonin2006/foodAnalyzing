package com.recipeservice.clients;

import com.recipeservice.dtos.internal.recipesdb.CardFullRecipeResponse;
import com.recipeservice.dtos.internal.recipesdb.CardRecipeRequest;
import com.recipeservice.dtos.internal.recipesdb.CardRecipeResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.List;

@HttpExchange("/api/recipes/db")
public interface RecipesDbClient {

    @PostExchange("/search")
    List<CardRecipeResponse> search(@RequestBody CardRecipeRequest request);

    @GetExchange("/{recipeId}")
    CardFullRecipeResponse getById(@PathVariable Long recipeId);
}
