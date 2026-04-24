package com.userservice.meals.model;

import com.userservice.meals.model.enums.MealSource;
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
@Table(name = "meal_entries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MealEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "calories", nullable = false)
    private Integer calories;

    @Column(name = "proteins")
    private Double proteins;

    @Column(name = "fats")
    private Double fats;

    @Column(name = "carbohydrates")
    private Double carbohydrates;

    @Column(name = "eaten_at", nullable = false)
    private LocalDateTime eatenAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false)
    private MealSource source;

    @Column(name = "amount_eaten", length = 80)
    private String amountEaten;

    @Column(name = "amount_mode", length = 32)
    private String amountMode;

    @Column(name = "eaten_ratio")
    private Double eatenRatio;

    @Column(name = "total_weight_grams")
    private Double totalWeightGrams;

    @Column(name = "eaten_weight_grams")
    private Double eatenWeightGrams;

    @Column(name = "package_fraction_numerator")
    private Integer packageFractionNumerator;

    @Column(name = "package_fraction_denominator")
    private Integer packageFractionDenominator;

    @Column(name = "full_portion_calories")
    private Integer fullPortionCalories;

    @Column(name = "full_portion_proteins")
    private Double fullPortionProteins;

    @Column(name = "full_portion_fats")
    private Double fullPortionFats;

    @Column(name = "full_portion_carbohydrates")
    private Double fullPortionCarbohydrates;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "image_url")
    private String imageUrl;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
