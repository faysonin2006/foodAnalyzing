package recipes.householdservice.household.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import recipes.householdservice.household.model.Household;

import java.util.UUID;

@Repository
public interface HouseholdRepository extends JpaRepository<Household, UUID> {
}
