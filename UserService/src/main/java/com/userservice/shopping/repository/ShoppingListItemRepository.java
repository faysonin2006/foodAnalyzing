package com.userservice.shopping.repository;

import com.userservice.shopping.model.ShoppingListItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShoppingListItemRepository extends JpaRepository<ShoppingListItem, UUID> {

    List<ShoppingListItem> findAllByUserIdOrderByCheckedAscCreatedAtDesc(UUID userId);

    Optional<ShoppingListItem> findByIdAndUserId(UUID id, UUID userId);
}
