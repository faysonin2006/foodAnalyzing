package com.userservice.pantry.dto;

import com.userservice.pantry.model.enums.PantryItemStatus;
import com.userservice.pantry.model.enums.PantryUnit;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdatePantryItemRequest {

    @NotBlank(message = "Product name is required")
    @Size(max = 120, message = "Product name must not exceed 120 characters")
    private String name;

    @Size(max = 120, message = "Brand must not exceed 120 characters")
    private String brand;

    @NotBlank(message = "Category is required")
    @Size(max = 60, message = "Category must not exceed 60 characters")
    private String category;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.01", message = "Quantity must be greater than 0")
    private BigDecimal quantity;

    @NotNull(message = "Unit is required")
    private PantryUnit unit;

    @NotNull(message = "Purchase date is required")
    private LocalDate purchasedAt;

    private LocalDate openedAt;

    private LocalDate expiresAt;

    private PantryItemStatus status;

    @Size(max = 500, message = "Image URL must not exceed 500 characters")
    private String imageUrl;

    @Size(max = 64, message = "Barcode must not exceed 64 characters")
    private String barcode;

    private Boolean rememberBarcode;

    @AssertTrue(message = "Opened date must not be before purchase date")
    public boolean isOpenedAtValid() {
        return openedAt == null || purchasedAt == null || !openedAt.isBefore(purchasedAt);
    }

    @AssertTrue(message = "Expiration date must not be before purchase date")
    public boolean isExpiresAtValid() {
        return expiresAt == null || purchasedAt == null || !expiresAt.isBefore(purchasedAt);
    }
}
