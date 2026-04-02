package com.userservice.pantry.dto;

import lombok.*;

import com.userservice.pantry.model.enums.PantryItemStatus;
import com.userservice.pantry.model.enums.PantryUnit;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class PantryListItemResponse {

    private UUID id;

    private String name;

    private String brand;

    private String category;

    private BigDecimal quantity;

    private PantryUnit unit;

    private LocalDate expiresAt;

    private PantryItemStatus status;

    private String imageUrl;
}
