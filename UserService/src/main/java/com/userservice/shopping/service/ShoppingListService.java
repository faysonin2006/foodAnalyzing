package com.userservice.shopping.service;

import com.userservice.common.constants.AppMessages;
import com.userservice.common.exceptions.ProfileNotFoundException;
import com.userservice.common.exceptions.ShoppingListItemNotFoundException;
import com.userservice.common.security.SecurityUtils;
import com.userservice.profile.repository.UserProfileRepository;
import com.userservice.shopping.dto.CreateShoppingListItemRequest;
import com.userservice.shopping.dto.ShoppingListItemResponse;
import com.userservice.shopping.mapper.ShoppingListItemMapper;
import com.userservice.shopping.model.ShoppingListItem;
import com.userservice.shopping.repository.ShoppingListItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShoppingListService {

    private final ShoppingListItemRepository shoppingListItemRepository;
    private final ShoppingListItemMapper shoppingListItemMapper;
    private final UserProfileRepository userProfileRepository;

    @Transactional
    public ShoppingListItemResponse createItem(CreateShoppingListItemRequest request) {
        return createItemForUser(resolveCurrentUserId(), request);
    }

    @Transactional
    public List<ShoppingListItemResponse> createItemsForEmail(String email, List<CreateShoppingListItemRequest> requests) {
        UUID userId = resolveUserId(email);
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }
        return requests.stream()
                .map(request -> createItemForUser(userId, request))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ShoppingListItemResponse> getItems() {
        return shoppingListItemRepository.findAllByUserIdOrderByCheckedAscCreatedAtDesc(resolveCurrentUserId())
                .stream()
                .map(shoppingListItemMapper::toResponse)
                .toList();
    }

    @Transactional
    public ShoppingListItemResponse toggleChecked(UUID itemId) {
        ShoppingListItem item = findOwnedItem(itemId);
        item.setChecked(!item.isChecked());
        return shoppingListItemMapper.toResponse(shoppingListItemRepository.save(item));
    }

    @Transactional
    public void deleteItem(UUID itemId) {
        shoppingListItemRepository.delete(findOwnedItem(itemId));
    }

    private ShoppingListItem findOwnedItem(UUID itemId) {
        UUID userId = resolveCurrentUserId();
        return shoppingListItemRepository.findByIdAndUserId(itemId, userId)
                .orElseThrow(() -> new ShoppingListItemNotFoundException(AppMessages.SHOPPING_LIST_ITEM_NOT_FOUND));
    }

    private UUID resolveCurrentUserId() {
        return resolveUserId(SecurityUtils.getCurrentUsername());
    }

    private UUID resolveUserId(String email) {
        return userProfileRepository.findByEmail(email)
                .orElseThrow(() -> new ProfileNotFoundException(AppMessages.PROFILE_NOT_FOUND))
                .getId();
    }

    private ShoppingListItemResponse createItemForUser(UUID userId, CreateShoppingListItemRequest request) {
        ShoppingListItem item = shoppingListItemMapper.toEntity(request);
        item.setUserId(userId);
        item.setChecked(false);
        return shoppingListItemMapper.toResponse(shoppingListItemRepository.save(item));
    }
}
