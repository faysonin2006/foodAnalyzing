package com.userservice.meals.controller;

import com.userservice.meals.controller.api.MealEntryControllerApi;
import com.userservice.meals.dto.CreateMealEntryRequest;
import com.userservice.meals.dto.MealEntryResponse;
import com.userservice.meals.dto.MealListItemResponse;
import com.userservice.meals.service.MealEntryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/meals")
@RequiredArgsConstructor
public class MealEntryController implements MealEntryControllerApi {

    private final MealEntryService mealEntryService;

    @Override
    @PostMapping
    public ResponseEntity<MealEntryResponse> createMeal(@RequestBody @Valid CreateMealEntryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mealEntryService.createMeal(request));
    }

    @Override
    @GetMapping
    public ResponseEntity<List<MealListItemResponse>> getMeals(
            @RequestParam(value = "dateFrom", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(value = "dateTo", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo
    ) {
        return ResponseEntity.ok(mealEntryService.getMeals(dateFrom, dateTo));
    }

    @Override
    @GetMapping("/{mealEntryId}")
    public ResponseEntity<MealEntryResponse> getMealById(@PathVariable UUID mealEntryId) {
        return ResponseEntity.ok(mealEntryService.getMealById(mealEntryId));
    }

    @Override
    @PutMapping("/{mealEntryId}")
    public ResponseEntity<MealEntryResponse> updateMeal(
            @PathVariable UUID mealEntryId,
            @RequestBody @Valid CreateMealEntryRequest request
    ) {
        return ResponseEntity.ok(mealEntryService.updateMeal(mealEntryId, request));
    }

    @Override
    @DeleteMapping("/{mealEntryId}")
    public ResponseEntity<Void> deleteMeal(@PathVariable UUID mealEntryId) {
        mealEntryService.deleteMeal(mealEntryId);
        return ResponseEntity.noContent().build();
    }
}
