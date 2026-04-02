package recipes.householdservice.household.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import recipes.householdservice.household.model.HouseholdMessage;

import java.util.List;
import java.util.UUID;

@Repository
public interface HouseholdMessageRepository extends JpaRepository<HouseholdMessage, UUID> {

    List<HouseholdMessage> findAllByHouseholdIdOrderByCreatedAtDesc(UUID householdId);
}
