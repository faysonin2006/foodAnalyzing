package com.userservice.pantry.controller;

import com.userservice.pantry.dto.PantryListItemResponse;
import com.userservice.pantry.service.PantryItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pantry/internal")
@RequiredArgsConstructor
public class InternalPantryController {

    private final PantryItemService pantryItemService;

    @GetMapping("/{email}")
    public ResponseEntity<List<PantryListItemResponse>> getActivePantryItemsForUser(@PathVariable String email) {
        return ResponseEntity.ok(pantryItemService.getActivePantryItemsForEmail(email));
    }
}
