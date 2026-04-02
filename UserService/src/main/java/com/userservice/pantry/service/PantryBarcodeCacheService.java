package com.userservice.pantry.service;

import com.userservice.pantry.model.PantryBarcodeCache;
import com.userservice.pantry.model.PantryItem;
import com.userservice.pantry.repository.PantryBarcodeCacheRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PantryBarcodeCacheService {

    private final PantryBarcodeCacheRepository pantryBarcodeCacheRepository;

    @Transactional(readOnly = true)
    public Optional<PantryBarcodeCache> findByUserIdAndBarcode(UUID userId, String barcode) {
        if (barcode == null || barcode.trim().isEmpty()) {
            return Optional.empty();
        }
        return pantryBarcodeCacheRepository.findByUserIdAndBarcode(userId, barcode.trim());
    }

    @Transactional
    public void rememberPantryItem(PantryItem pantryItem, String source) {
        final String barcode = pantryItem.getBarcode() == null ? "" : pantryItem.getBarcode().trim();
        if (barcode.isEmpty()) {
            return;
        }

        final PantryBarcodeCache cache = pantryBarcodeCacheRepository
                .findByUserIdAndBarcode(pantryItem.getUserId(), barcode)
                .orElseGet(PantryBarcodeCache::new);

        cache.setUserId(pantryItem.getUserId());
        cache.setBarcode(barcode);
        cache.setName(trimToNull(pantryItem.getName()));
        cache.setBrand(trimToNull(pantryItem.getBrand()));
        cache.setCategory(trimToNull(pantryItem.getCategory()));
        cache.setImageUrl(trimToNull(pantryItem.getImageUrl()));
        cache.setQuantity(pantryItem.getQuantity());
        cache.setUnit(pantryItem.getUnit());
        cache.setRawQuantity(buildRawQuantity(pantryItem));
        cache.setCreatedSource(trimToNull(source));

        pantryBarcodeCacheRepository.save(cache);
    }

    @Transactional
    public void refreshImageIfRemembered(PantryItem pantryItem) {
        final String barcode = pantryItem.getBarcode() == null ? "" : pantryItem.getBarcode().trim();
        if (barcode.isEmpty()) {
            return;
        }

        pantryBarcodeCacheRepository.findByUserIdAndBarcode(pantryItem.getUserId(), barcode)
                .ifPresent(cache -> {
                    cache.setImageUrl(trimToNull(pantryItem.getImageUrl()));
                    pantryBarcodeCacheRepository.save(cache);
                });
    }

    private String buildRawQuantity(PantryItem pantryItem) {
        if (pantryItem.getQuantity() == null || pantryItem.getUnit() == null) {
            return null;
        }
        return pantryItem.getQuantity().stripTrailingZeros().toPlainString()
                + " "
                + pantryItem.getUnit().name().toLowerCase().replace('_', ' ');
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        final String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
