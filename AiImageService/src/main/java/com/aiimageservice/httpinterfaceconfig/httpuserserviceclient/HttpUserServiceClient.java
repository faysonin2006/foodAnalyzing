package com.aiimageservice.httpinterfaceconfig.httpuserserviceclient;

import com.aiimageservice.dtos.profiles.UserProfileResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange("/api/profiles")
public interface HttpUserServiceClient {

    @GetExchange("/internal/{email}")
    UserProfileResponse getUserProfileById(@PathVariable String email);
}
