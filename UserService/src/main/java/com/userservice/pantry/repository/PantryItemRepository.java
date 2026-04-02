package com.userservice.pantry.repository;

import com.userservice.pantry.model.PantryItem;
import com.userservice.pantry.model.enums.PantryItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PantryItemRepository extends JpaRepository<PantryItem, UUID> {

    List<PantryItem> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<PantryItem> findByIdAndUserId(UUID pantryItemId, UUID userId);

    List<PantryItem> findAllByUserIdAndStatus(UUID userId, PantryItemStatus status);

    List<PantryItem> findAllByUserIdAndStatusNotOrderByCreatedAtDesc(UUID userId, PantryItemStatus status);

    List<PantryItem> findAllByUserIdAndStatusAndExpiresAtBetween(
            UUID userId,
            PantryItemStatus status,
            LocalDate startDate,
            LocalDate endDate
    );

    List<PantryItem> findAllByUserIdAndStatusAndExpiresAtBefore(
            UUID userId,
            PantryItemStatus status,
            LocalDate date
    );
}
