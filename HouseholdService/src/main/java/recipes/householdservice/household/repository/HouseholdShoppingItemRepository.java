package recipes.householdservice.household.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import recipes.householdservice.household.model.HouseholdShoppingItem;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HouseholdShoppingItemRepository extends JpaRepository<HouseholdShoppingItem, UUID> {

    List<HouseholdShoppingItem> findAllByHouseholdIdOrderByCheckedAscCreatedAtDesc(UUID householdId);

    Optional<HouseholdShoppingItem> findByIdAndHouseholdId(UUID id, UUID householdId);

    long countByHouseholdIdAndCheckedFalse(UUID householdId);
}
