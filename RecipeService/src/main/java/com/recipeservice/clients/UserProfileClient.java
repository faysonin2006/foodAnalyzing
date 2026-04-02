package com.recipeservice.clients;

import com.recipeservice.dtos.internal.profile.UserProfileResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange("/api/profiles/internal")
public interface UserProfileClient {

    @GetExchange("/{email}")
    UserProfileResponse getProfile(@PathVariable String email);
}
