package com.authservice.config.httpinterfaceconfig.userserviceclient;

import com.authservice.dtos.profie.CreateProfileRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange("/api/profiles")
public interface HttpUserServiceClient {

    @PostExchange
    ResponseEntity<Void> createProfile(@RequestBody CreateProfileRequest request);

}
