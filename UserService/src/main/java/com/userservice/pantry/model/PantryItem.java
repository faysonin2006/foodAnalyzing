package com.userservice.pantry.model;

import com.userservice.pantry.model.enums.PantryItemStatus;
import com.userservice.pantry.model.enums.PantryUnit;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "pantry_items")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PantryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "brand", nullable = true)
    private String brand;

    @Column(name = "category", nullable = false)
    private String category;

    @Column(name = "quantity", nullable = false)
    private BigDecimal quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit", nullable = false)
    private PantryUnit unit;

    @Column(name = "purchased_at", nullable = false)
    private LocalDate purchasedAt;

    @Column(name = "opened_at", nullable = true)
    private LocalDate openedAt;

    @Column(name = "expires_at", nullable = true)
    private LocalDate expiresAt;

    @Column(name = "status")
    @Enumerated(value = EnumType.STRING)
    private PantryItemStatus status;

    @Column(name = "image_url", nullable = true)
    private String imageUrl;

    @Column(name = "barcode", nullable = true)
    private String barcode;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

}
