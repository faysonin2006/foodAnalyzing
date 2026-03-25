package com.aiimageservice.controllers;

import com.aiimageservice.controllers.api.FoodAnalysisControllerApi;
import com.aiimageservice.dtos.FoodAnalysisDetailResponse;
import com.aiimageservice.dtos.FoodAnalysisResponse;
import com.aiimageservice.services.FoodAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/food")
@RequiredArgsConstructor
public class FoodAnalysisController implements FoodAnalysisControllerApi {

    private final FoodAnalysisService service;

    @Override
    @PostMapping("/analyze")
    public ResponseEntity<FoodAnalysisResponse> analyzeFood(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "extraQuestions", required = false, defaultValue = "") String questions
    ) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(service.uploadAndAnalyze(file, questions));
    }

    @Override
    @GetMapping("/analysis/{id}")
    public ResponseEntity<FoodAnalysisDetailResponse> getAnalysis(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getAnalysisById(id));
    }

    @Override
    @GetMapping("/history")
    public ResponseEntity<List<FoodAnalysisResponse>> getHistory() {
        return ResponseEntity.ok(service.getUserHistory());
    }
}
