package com.aiimageservice.controllers;

import com.aiimageservice.dtos.FoodAnalysisDetailResponse;
import com.aiimageservice.dtos.FoodAnalysisResponse;
import com.aiimageservice.services.FoodAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/food")
@RequiredArgsConstructor
public class FoodAnalysisController {

    private final FoodAnalysisService service;

    @PostMapping("/analyze")
    public ResponseEntity<FoodAnalysisResponse> analyzeFood(
            @RequestParam("file") MultipartFile file, Authentication authentication,
            @RequestParam("extraQuestions") String questions
    ) {
        String userId = authentication.getName();
        return ResponseEntity.ok(service.uploadAndAnalyze(file, userId, questions));
    }

    @GetMapping("/analysis/{id}")
    public ResponseEntity<FoodAnalysisDetailResponse> getAnalysis(
            @PathVariable UUID id,
            Authentication auth
    ) {
        return ResponseEntity.ok(service.getAnalysisById(id, auth.getName()));
    }

    @GetMapping("/history")
    public ResponseEntity<List<FoodAnalysisResponse>> getHistory(Authentication auth) {
        return ResponseEntity.ok(service.getUserHistory(auth.getName()));
    }
}
