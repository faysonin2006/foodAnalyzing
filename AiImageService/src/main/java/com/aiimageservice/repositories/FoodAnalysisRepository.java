package com.aiimageservice.repositories;

import com.aiimageservice.models.FoodAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FoodAnalysisRepository extends JpaRepository<FoodAnalysis, UUID> {
    List<FoodAnalysis> findByUserIdOrderByCreatedAtDesc(String userId);
}
