package com.aiimageservice.models;

import com.aiimageservice.models.enums.AnalysisStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "food_analysis")
@Data
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

    @Column(name = "extra_info", columnDefinition = "TEXT")
    private String extraInfo;

    @Column(name = "error_message")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
