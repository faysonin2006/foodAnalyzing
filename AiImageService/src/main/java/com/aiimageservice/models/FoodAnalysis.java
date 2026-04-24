package com.aiimageservice.models;

import com.aiimageservice.models.enums.AnalysisStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "food_analysis")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AnalysisStatus status;

    @Column(name = "dish_name")
    private String dishName;

    @Column(name = "calories")
    private Integer calories;

    @Column(name = "protein")
    private Double protein;

    @Column(name = "carbs")
    private Double carbs;

    @Column(name = "fats")
    private Double fats;

    @Column(name = "is_food")
    private Boolean foodDetected;

    @Column(name = "health_score")
    private Integer healthScore;

    @Column(name = "estimated_weight_grams")
    private Integer estimatedWeightGrams;

    @Column(name = "extra_info", columnDefinition = "TEXT")
    private String extraInfo;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "analysis_basis", length = 32)
    private String analysisBasis;

    @Column(name = "saved_meal_id")
    private UUID savedMealId;

    @Column(name = "saved_at")
    private LocalDateTime savedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
