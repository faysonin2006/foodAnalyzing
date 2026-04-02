package com.userservice.pantry.controller;

import com.userservice.pantry.controller.api.PantryControllerApi;
import com.userservice.pantry.dto.CreatePantryItemRequest;
import com.userservice.pantry.dto.PantryBarcodeLookupResponse;
import com.userservice.pantry.dto.PantryItemResponse;
import com.userservice.pantry.dto.PantryListItemResponse;
import com.userservice.pantry.dto.UpdatePantryItemRequest;
import com.userservice.pantry.service.PantryBarcodeLookupService;
import com.userservice.pantry.service.PantryItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/pantry")
@RequiredArgsConstructor
public class PantryController implements PantryControllerApi {

    private final PantryItemService pantryItemService;
    private final PantryBarcodeLookupService pantryBarcodeLookupService;

    @Override
    @PostMapping
    public ResponseEntity<PantryItemResponse> createPantryItem(@RequestBody @Valid CreatePantryItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pantryItemService.createPantryItem(request));
    }

    @Override
    @GetMapping
    public ResponseEntity<List<PantryListItemResponse>> getPantryItems() {
        return ResponseEntity.ok(pantryItemService.getPantryItems());
    }

    @Override
    @GetMapping("/barcode/{barcode}")
    public ResponseEntity<PantryBarcodeLookupResponse> lookupProductByBarcode(@PathVariable String barcode) {
        return ResponseEntity.ok(pantryBarcodeLookupService.lookupByBarcode(barcode));
    }

    @Override
    @GetMapping("/{pantryItemId}")
    public ResponseEntity<PantryItemResponse> getPantryItemById(@PathVariable UUID pantryItemId) {
        return ResponseEntity.ok(pantryItemService.getPantryItemById(pantryItemId));
    }

    @Override
    @PutMapping("/{pantryItemId}")
    public ResponseEntity<PantryItemResponse> updatePantryItem(
            @PathVariable UUID pantryItemId,
            @RequestBody @Valid UpdatePantryItemRequest request
    ) {
        return ResponseEntity.ok(pantryItemService.updatePantryItem(pantryItemId, request));
    }

    @Override
    @PostMapping(value = "/{pantryItemId}/image", consumes = "multipart/form-data")
    public ResponseEntity<PantryItemResponse> uploadPantryItemImage(
            @PathVariable UUID pantryItemId,
            @RequestParam("file") MultipartFile file
    ) {
        return ResponseEntity.ok(pantryItemService.uploadPantryItemImage(pantryItemId, file));
    }

    @Override
    @DeleteMapping("/{pantryItemId}")
    public ResponseEntity<Void> deletePantryItem(@PathVariable UUID pantryItemId) {
        pantryItemService.deletePantryItem(pantryItemId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @GetMapping("/expiring-soon")
    public ResponseEntity<List<PantryListItemResponse>> getExpiringSoonItems() {
        return ResponseEntity.ok(pantryItemService.getExpiringSoonItems());
    }

    @Override
    @GetMapping("/expired")
    public ResponseEntity<List<PantryListItemResponse>> getExpiredItems() {
        return ResponseEntity.ok(pantryItemService.getExpiredItems());
    }
}
