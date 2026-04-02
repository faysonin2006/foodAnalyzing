package com.userservice.shopping.controller;

import com.userservice.shopping.controller.api.ShoppingListControllerApi;
import com.userservice.shopping.dto.CreateShoppingListItemRequest;
import com.userservice.shopping.dto.ShoppingListItemResponse;
import com.userservice.shopping.service.ShoppingListService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/shopping-lists/items")
@RequiredArgsConstructor
public class ShoppingListController implements ShoppingListControllerApi {

    private final ShoppingListService shoppingListService;

    @Override
    @PostMapping
    public ResponseEntity<ShoppingListItemResponse> createItem(@RequestBody @Valid CreateShoppingListItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(shoppingListService.createItem(request));
    }

    @Override
    @GetMapping
    public ResponseEntity<List<ShoppingListItemResponse>> getItems() {
        return ResponseEntity.ok(shoppingListService.getItems());
    }

    @Override
    @PatchMapping("/{itemId}/check")
    public ResponseEntity<ShoppingListItemResponse> toggleChecked(@PathVariable UUID itemId) {
        return ResponseEntity.ok(shoppingListService.toggleChecked(itemId));
    }

    @Override
    @DeleteMapping("/{itemId}")
    public ResponseEntity<Void> deleteItem(@PathVariable UUID itemId) {
        shoppingListService.deleteItem(itemId);
        return ResponseEntity.noContent().build();
    }
}
