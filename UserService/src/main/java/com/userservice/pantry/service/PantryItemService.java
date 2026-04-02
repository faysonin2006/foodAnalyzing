package com.userservice.pantry.service;

import com.userservice.common.constants.AppMessages;
import com.userservice.common.exceptions.PantryItemNotFoundException;
import com.userservice.common.exceptions.ProfileNotFoundException;
import com.userservice.common.exceptions.StorageException;
import com.userservice.common.security.SecurityUtils;
import com.userservice.pantry.dto.CreatePantryItemRequest;
import com.userservice.pantry.dto.PantryItemResponse;
import com.userservice.pantry.dto.PantryListItemResponse;
import com.userservice.pantry.dto.UpdatePantryItemRequest;
import com.userservice.pantry.mapper.PantryItemMapper;
import com.userservice.pantry.model.PantryItem;
import com.userservice.pantry.model.enums.PantryItemStatus;
import com.userservice.pantry.repository.PantryItemRepository;
import com.userservice.profile.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PantryItemService {

    private final PantryItemRepository pantryItemRepository;
    private final PantryItemMapper pantryItemMapper;
    private final UserProfileRepository userProfileRepository;
    private final PantryImageStorageService pantryImageStorageService;
    private final PantryBarcodeCacheService pantryBarcodeCacheService;

    @Value("${pantry.expiring-soon-threshold-days:3}")
    private int expiringSoonThresholdDays;

    @Transactional
    public PantryItemResponse createPantryItem(CreatePantryItemRequest request) {
        PantryItem pantryItem = pantryItemMapper.toEntity(request);
        pantryItem.setUserId(resolveCurrentUserId());
        pantryItem.setStatus(resolveStatus(null, pantryItem.getExpiresAt()));
        PantryItem saved = pantryItemRepository.save(pantryItem);
        if (Boolean.TRUE.equals(request.getRememberBarcode())) {
            pantryBarcodeCacheService.rememberPantryItem(saved, "USER_INPUT");
        }
        return pantryItemMapper.toResponse(saved);
    }

    @Transactional
    public List<PantryListItemResponse> getPantryItems() {
        UUID userId = resolveCurrentUserId();
        syncExpiredStatuses(userId);
        return pantryItemRepository.findAllByUserIdAndStatusNotOrderByCreatedAtDesc(userId, PantryItemStatus.REMOVED)
                .stream()
                .map(pantryItemMapper::toListItemResponse)
                .toList();
    }

    @Transactional
    public PantryItemResponse getPantryItemById(UUID pantryItemId) {
        UUID userId = resolveCurrentUserId();
        PantryItem pantryItem = findAccessiblePantryItem(pantryItemId, userId);
        refreshExpiredStatusIfNeeded(pantryItem);
        return pantryItemMapper.toResponse(pantryItem);
    }

    @Transactional
    public PantryItemResponse updatePantryItem(UUID pantryItemId, UpdatePantryItemRequest request) {
        UUID userId = resolveCurrentUserId();
        PantryItem pantryItem = findAccessiblePantryItem(pantryItemId, userId);
        pantryItemMapper.updateFromRequest(request, pantryItem);
        pantryItem.setStatus(resolveStatus(request.getStatus(), pantryItem.getExpiresAt()));
        PantryItem saved = pantryItemRepository.save(pantryItem);
        if (Boolean.TRUE.equals(request.getRememberBarcode())) {
            pantryBarcodeCacheService.rememberPantryItem(saved, "USER_INPUT");
        }
        return pantryItemMapper.toResponse(saved);
    }

    @Transactional
    public PantryItemResponse uploadPantryItemImage(UUID pantryItemId, MultipartFile file) {
        validateImageFile(file);

        UUID userId = resolveCurrentUserId();
        PantryItem pantryItem = findAccessiblePantryItem(pantryItemId, userId);

        try {
            String imageUrl = pantryImageStorageService.uploadImage(file, userId, pantryItemId);
            pantryItem.setImageUrl(imageUrl);
            PantryItem saved = pantryItemRepository.save(pantryItem);
            pantryBarcodeCacheService.refreshImageIfRemembered(saved);
            return pantryItemMapper.toResponse(saved);
        } catch (Exception exception) {
            throw new StorageException(AppMessages.FAILED_TO_UPLOAD_PANTRY_IMAGE, exception);
        }
    }

    @Transactional
    public void deletePantryItem(UUID pantryItemId) {
        UUID userId = resolveCurrentUserId();
        PantryItem pantryItem = findAccessiblePantryItem(pantryItemId, userId);
        pantryItem.setStatus(PantryItemStatus.REMOVED);
        pantryItemRepository.save(pantryItem);
    }

    @Transactional
    public List<PantryListItemResponse> getExpiringSoonItems() {
        UUID userId = resolveCurrentUserId();
        syncExpiredStatuses(userId);
        LocalDate today = LocalDate.now();
        LocalDate thresholdDate = today.plusDays(Math.max(expiringSoonThresholdDays, 0));
        return pantryItemRepository.findAllByUserIdAndStatusAndExpiresAtBetween(
                        userId,
                        PantryItemStatus.ACTIVE,
                        today,
                        thresholdDate
                ).stream()
                .map(pantryItemMapper::toListItemResponse)
                .toList();
    }

    @Transactional
    public List<PantryListItemResponse> getExpiredItems() {
        UUID userId = resolveCurrentUserId();
        syncExpiredStatuses(userId);
        return pantryItemRepository.findAllByUserIdAndStatus(userId, PantryItemStatus.EXPIRED)
                .stream()
                .map(pantryItemMapper::toListItemResponse)
                .toList();
    }

    @Transactional
    public List<PantryListItemResponse> getActivePantryItemsForEmail(String email) {
        UUID userId = resolveUserIdByEmail(email);
        syncExpiredStatuses(userId);
        return pantryItemRepository.findAllByUserIdAndStatus(userId, PantryItemStatus.ACTIVE)
                .stream()
                .map(pantryItemMapper::toListItemResponse)
                .toList();
    }

    private UUID resolveCurrentUserId() {
        return resolveUserIdByEmail(SecurityUtils.getCurrentUsername());
    }

    private UUID resolveUserIdByEmail(String email) {
        return userProfileRepository.findByEmail(email)
                .orElseThrow(() -> new ProfileNotFoundException(AppMessages.PROFILE_NOT_FOUND))
                .getId();
    }

    private PantryItem findAccessiblePantryItem(UUID pantryItemId, UUID userId) {
        PantryItem pantryItem = pantryItemRepository.findByIdAndUserId(pantryItemId, userId)
                .orElseThrow(() -> new PantryItemNotFoundException(AppMessages.PANTRY_ITEM_NOT_FOUND));
        if (pantryItem.getStatus() == PantryItemStatus.REMOVED) {
            throw new PantryItemNotFoundException(AppMessages.PANTRY_ITEM_NOT_FOUND);
        }
        return pantryItem;
    }

    private void syncExpiredStatuses(UUID userId) {
        List<PantryItem> expiredActiveItems = pantryItemRepository.findAllByUserIdAndStatusAndExpiresAtBefore(
                userId,
                PantryItemStatus.ACTIVE,
                LocalDate.now()
        );

        if (expiredActiveItems.isEmpty()) {
            return;
        }

        expiredActiveItems.forEach(item -> item.setStatus(PantryItemStatus.EXPIRED));
        pantryItemRepository.saveAll(expiredActiveItems);
    }

    private void refreshExpiredStatusIfNeeded(PantryItem pantryItem) {
        if (pantryItem.getStatus() == PantryItemStatus.ACTIVE
                && pantryItem.getExpiresAt() != null
                && pantryItem.getExpiresAt().isBefore(LocalDate.now())) {
            pantryItem.setStatus(PantryItemStatus.EXPIRED);
            pantryItemRepository.save(pantryItem);
        }
    }

    private PantryItemStatus resolveStatus(PantryItemStatus requestedStatus, LocalDate expiresAt) {
        if (requestedStatus == PantryItemStatus.REMOVED || requestedStatus == PantryItemStatus.CONSUMED) {
            return requestedStatus;
        }
        if (expiresAt != null && expiresAt.isBefore(LocalDate.now())) {
            return PantryItemStatus.EXPIRED;
        }
        return PantryItemStatus.ACTIVE;
    }

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new com.userservice.common.exceptions.BadRequestException(AppMessages.FILE_MUST_NOT_BE_EMPTY);
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new com.userservice.common.exceptions.BadRequestException(AppMessages.INVALID_IMAGE_FILE);
        }
    }
}
