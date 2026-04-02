package com.userservice.shopping.controller;

import com.userservice.shopping.dto.CreateShoppingListItemRequest;
import com.userservice.shopping.dto.ShoppingListItemResponse;
import com.userservice.shopping.service.ShoppingListService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/shopping-lists/internal")
@RequiredArgsConstructor
public class InternalShoppingListController {

    private final ShoppingListService shoppingListService;

    @PostMapping("/{email}/items")
    public ResponseEntity<List<ShoppingListItemResponse>> createItemsForUser(
            @PathVariable String email,
            @RequestBody List<@Valid CreateShoppingListItemRequest> requests
    ) {
        return ResponseEntity.ok(shoppingListService.createItemsForEmail(email, requests));
    }
}
