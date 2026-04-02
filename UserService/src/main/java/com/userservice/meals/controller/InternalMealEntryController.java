package com.userservice.meals.controller;

import com.userservice.meals.dto.CreateMealEntryRequest;
import com.userservice.meals.dto.MealEntryResponse;
import com.userservice.meals.service.MealEntryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/meals/internal")
@RequiredArgsConstructor
public class InternalMealEntryController {

    private final MealEntryService mealEntryService;

    @PostMapping("/{email}")
    public ResponseEntity<MealEntryResponse> createMealForUser(
            @PathVariable String email,
            @RequestBody @Valid CreateMealEntryRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mealEntryService.createMealForEmail(email, request));
    }
}
