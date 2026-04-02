package com.userservice.pantry.repository;

import com.userservice.pantry.model.PantryBarcodeCache;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PantryBarcodeCacheRepository extends JpaRepository<PantryBarcodeCache, UUID> {

    Optional<PantryBarcodeCache> findByUserIdAndBarcode(UUID userId, String barcode);
}
