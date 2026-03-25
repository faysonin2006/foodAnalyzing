package com.recipeservice.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recipeservice.dtos.spoonacular.complexSearch.SpoonacularRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Map;

@Component
public class SpoonacularRequestMapper {

    private final ObjectMapper objectMapper;

    public SpoonacularRequestMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public MultiValueMap<String, String> toMap(SpoonacularRequest request) {
        MultiValueMap<String, String> multiMap = new LinkedMultiValueMap<>();

        Map<String, Object> map = objectMapper.convertValue(request, new TypeReference<>() {});

        map.forEach((key, value) -> {
            if (value != null) {
                multiMap.add(key, String.valueOf(value));
            }
        });

        return multiMap;
    }
}
