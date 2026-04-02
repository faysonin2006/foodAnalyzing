package com.userservice.pantry.dto;

import com.userservice.pantry.model.enums.PantryItemStatus;
import com.userservice.pantry.model.enums.PantryUnit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PantryItemResponse {
    private UUID id;

    private String name;

    private String brand;

    private String category;

    private BigDecimal quantity;

    private PantryUnit unit;

    private LocalDate purchasedAt;

    private LocalDate openedAt;

    private LocalDate expiresAt;

    private PantryItemStatus status;

    private String imageUrl;

    private String barcode;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
