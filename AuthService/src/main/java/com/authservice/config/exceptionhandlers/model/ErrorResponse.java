package com.authservice.config.exceptionhandlers.model;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.Map;

@Value
@Builder
public class ErrorResponse {

   Instant timestamp;

   String message;

   String error;

   int status;

   String path;

   Map<String, String> validationErrors;
}
