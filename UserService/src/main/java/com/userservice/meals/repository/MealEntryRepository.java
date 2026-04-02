package com.userservice.meals.repository;

import com.userservice.meals.model.MealEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MealEntryRepository extends JpaRepository<MealEntry, UUID> {

    List<MealEntry> findAllByUserIdOrderByEatenAtDesc(UUID userId);

    List<MealEntry> findAllByUserIdAndEatenAtBetweenOrderByEatenAtDesc(
            UUID userId,
            LocalDateTime dateFrom,
            LocalDateTime dateTo
    );

    Optional<MealEntry> findByIdAndUserId(UUID mealEntryId, UUID userId);
}
