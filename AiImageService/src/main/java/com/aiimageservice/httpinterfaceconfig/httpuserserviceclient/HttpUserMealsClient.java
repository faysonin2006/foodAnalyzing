package com.aiimageservice.httpinterfaceconfig.httpuserserviceclient;

import com.aiimageservice.dtos.meals.CreateMealEntryInternalRequest;
import com.aiimageservice.dtos.meals.MealEntryResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange("/api/meals/internal")
public interface HttpUserMealsClient {

    @PostExchange("/{email}")
    MealEntryResponse createMealForUser(@PathVariable String email, @RequestBody CreateMealEntryInternalRequest request);
}
