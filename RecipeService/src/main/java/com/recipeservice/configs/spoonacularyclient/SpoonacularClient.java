package com.recipeservice.configs.spoonacularyclient;

import com.recipeservice.dtos.spoonacular.SpoonAnalyzedInstructionDto;
import com.recipeservice.dtos.spoonacular.complexSearch.SpoonacularResponse;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.List;

@HttpExchange("https://api.spoonacular.com")
public interface SpoonacularClient {

    @GetExchange("/recipes/complexSearch")
    public SpoonacularResponse complexSearch(@RequestParam MultiValueMap<String, String> params);

    @GetExchange("/recipes/{id}/analyzedInstructions")
    public List<SpoonAnalyzedInstructionDto> getAnalyzedInstructions(@PathVariable String id);
}
