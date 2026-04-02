package recipes.householdservice.household.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import recipes.householdservice.household.model.HouseholdMember;
import recipes.householdservice.household.model.modelenums.HouseholdRole;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HouseholdMemberRepository extends JpaRepository<HouseholdMember, UUID> {

    List<HouseholdMember> findAllByUserIdOrderByJoinedAtDesc(UUID userId);

    List<HouseholdMember> findAllByHouseholdIdOrderByJoinedAtAsc(UUID householdId);

    Optional<HouseholdMember> findByHouseholdIdAndUserId(UUID householdId, UUID userId);

    Optional<HouseholdMember> findByHouseholdIdAndRole(UUID householdId, HouseholdRole role);

    boolean existsByHouseholdIdAndUserId(UUID householdId, UUID userId);

    long countByHouseholdId(UUID householdId);
}
