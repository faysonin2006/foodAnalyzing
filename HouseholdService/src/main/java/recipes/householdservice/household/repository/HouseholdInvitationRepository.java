package recipes.householdservice.household.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import recipes.householdservice.household.model.HouseholdInvitation;
import recipes.householdservice.household.model.modelenums.HouseholdInvitationStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HouseholdInvitationRepository extends JpaRepository<HouseholdInvitation, UUID> {

    boolean existsByHouseholdIdAndInvitedUserIdAndStatus(UUID householdId, UUID invitedUserId, HouseholdInvitationStatus status);

    List<HouseholdInvitation> findAllByInvitedUserIdAndStatusOrderByCreatedAtDesc(UUID invitedUserId, HouseholdInvitationStatus status);

    Optional<HouseholdInvitation> findByIdAndInvitedUserId(UUID id, UUID invitedUserId);
}
