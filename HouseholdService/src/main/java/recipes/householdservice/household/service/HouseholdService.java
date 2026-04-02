package recipes.householdservice.household.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import recipes.householdservice.clients.UserProfileClient;
import recipes.householdservice.common.constants.AppMessages;
import recipes.householdservice.common.exceptions.ConflictException;
import recipes.householdservice.common.exceptions.ForbiddenOperationException;
import recipes.householdservice.common.exceptions.HouseholdInvitationNotFoundException;
import recipes.householdservice.common.exceptions.HouseholdMemberNotFoundException;
import recipes.householdservice.common.exceptions.HouseholdNotFoundException;
import recipes.householdservice.common.exceptions.HouseholdShoppingItemNotFoundException;
import recipes.householdservice.common.exceptions.UpstreamServiceException;
import recipes.householdservice.common.security.SecurityUtils;
import recipes.householdservice.dto.household.AddHouseholdMemberRequest;
import recipes.householdservice.dto.household.CreateHouseholdMessageRequest;
import recipes.householdservice.dto.household.CreateHouseholdRequest;
import recipes.householdservice.dto.household.CreateHouseholdInvitationRequest;
import recipes.householdservice.dto.household.CreateHouseholdShoppingItemRequest;
import recipes.householdservice.dto.household.HouseholdInvitationResponse;
import recipes.householdservice.dto.household.HouseholdMemberResponse;
import recipes.householdservice.dto.household.HouseholdMessageResponse;
import recipes.householdservice.dto.household.HouseholdResponse;
import recipes.householdservice.dto.household.HouseholdShoppingItemResponse;
import recipes.householdservice.dto.household.HouseholdSummaryResponse;
import recipes.householdservice.dto.internal.profile.UserProfileResponse;
import recipes.householdservice.household.mapper.HouseholdMapper;
import recipes.householdservice.household.mapper.HouseholdInvitationMapper;
import recipes.householdservice.household.mapper.HouseholdMemberMapper;
import recipes.householdservice.household.mapper.HouseholdMessageMapper;
import recipes.householdservice.household.mapper.HouseholdShoppingItemMapper;
import recipes.householdservice.household.model.Household;
import recipes.householdservice.household.model.HouseholdInvitation;
import recipes.householdservice.household.model.HouseholdMember;
import recipes.householdservice.household.model.HouseholdMessage;
import recipes.householdservice.household.model.HouseholdShoppingItem;
import recipes.householdservice.household.model.modelenums.HouseholdInvitationStatus;
import recipes.householdservice.household.model.modelenums.HouseholdMessageType;
import recipes.householdservice.household.model.modelenums.HouseholdRole;
import recipes.householdservice.household.repository.HouseholdInvitationRepository;
import recipes.householdservice.household.repository.HouseholdMemberRepository;
import recipes.householdservice.household.repository.HouseholdMessageRepository;
import recipes.householdservice.household.repository.HouseholdRepository;
import recipes.householdservice.household.repository.HouseholdShoppingItemRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HouseholdService {

    private final HouseholdRepository householdRepository;
    private final HouseholdMemberRepository householdMemberRepository;
    private final HouseholdInvitationRepository householdInvitationRepository;
    private final HouseholdShoppingItemRepository householdShoppingItemRepository;
    private final HouseholdMessageRepository householdMessageRepository;
    private final HouseholdMapper householdMapper;
    private final HouseholdInvitationMapper householdInvitationMapper;
    private final HouseholdMemberMapper householdMemberMapper;
    private final HouseholdShoppingItemMapper householdShoppingItemMapper;
    private final HouseholdMessageMapper householdMessageMapper;
    private final UserProfileClient userProfileClient;

    @Transactional
    public HouseholdResponse createHousehold(CreateHouseholdRequest request) {
        Actor actor = currentActor();

        Household household = householdRepository.save(Household.builder()
                .name(request.getName().trim())
                .createdByUserId(actor.userId())
                .createdByEmail(actor.email())
                .build());

        householdMemberRepository.save(HouseholdMember.builder()
                .householdId(household.getId())
                .userId(actor.userId())
                .email(actor.email())
                .displayName(actor.displayName())
                .role(HouseholdRole.OWNER)
                .build());

        createSystemMessage(household.getId(), actor.displayName() + " created household " + household.getName());
        return getHousehold(household.getId());
    }

    @Transactional(readOnly = true)
    public List<HouseholdSummaryResponse> getMyHouseholds() {
        Actor actor = currentActor();
        return householdMemberRepository.findAllByUserIdOrderByJoinedAtDesc(actor.userId()).stream()
                .map(member -> {
                    Household household = findHousehold(member.getHouseholdId());
                    return householdMapper.toSummary(
                            household,
                            householdMemberRepository.countByHouseholdId(household.getId()),
                            householdShoppingItemRepository.countByHouseholdIdAndCheckedFalse(household.getId())
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public HouseholdResponse getHousehold(UUID householdId) {
        Actor actor = currentActor();
        ensureMembership(householdId, actor.userId());
        Household household = findHousehold(householdId);
        List<HouseholdMemberResponse> members = householdMemberRepository.findAllByHouseholdIdOrderByJoinedAtAsc(householdId).stream()
                .map(householdMemberMapper::toResponse)
                .toList();
        long uncheckedItemsCount = householdShoppingItemRepository.countByHouseholdIdAndCheckedFalse(householdId);
        return householdMapper.toResponse(household, members, uncheckedItemsCount);
    }

    @Transactional
    public HouseholdMemberResponse addMember(UUID householdId, AddHouseholdMemberRequest request) {
        Actor actor = currentActor();
        ensureOwner(householdId, actor.userId());

        UserProfileResponse targetProfile = fetchProfile(request.getEmail().trim());
        if (householdMemberRepository.existsByHouseholdIdAndUserId(householdId, targetProfile.getId())) {
            throw new ConflictException(AppMessages.USER_ALREADY_IN_HOUSEHOLD);
        }

        HouseholdMember member = householdMemberRepository.save(HouseholdMember.builder()
                .householdId(householdId)
                .userId(targetProfile.getId())
                .email(targetProfile.getEmail())
                .displayName(resolveDisplayName(targetProfile))
                .role(HouseholdRole.MEMBER)
                .build());

        createSystemMessage(householdId, actor.displayName() + " added " + resolveDisplayName(targetProfile) + " to household");
        return householdMemberMapper.toResponse(member);
    }

    @Transactional
    public HouseholdInvitationResponse createInvitation(UUID householdId, CreateHouseholdInvitationRequest request) {
        Actor actor = currentActor();
        ensureOwner(householdId, actor.userId());

        UserProfileResponse targetProfile = fetchProfile(request.getEmail().trim());
        if (targetProfile.getId().equals(actor.userId())) {
            throw new ConflictException(AppMessages.CANNOT_INVITE_SELF);
        }
        if (householdMemberRepository.existsByHouseholdIdAndUserId(householdId, targetProfile.getId())) {
            throw new ConflictException(AppMessages.USER_ALREADY_IN_HOUSEHOLD);
        }
        if (householdInvitationRepository.existsByHouseholdIdAndInvitedUserIdAndStatus(
                householdId,
                targetProfile.getId(),
                HouseholdInvitationStatus.PENDING
        )) {
            throw new ConflictException(AppMessages.HOUSEHOLD_INVITATION_ALREADY_EXISTS);
        }

        Household household = findHousehold(householdId);
        HouseholdInvitation invitation = householdInvitationRepository.save(HouseholdInvitation.builder()
                .householdId(householdId)
                .invitedUserId(targetProfile.getId())
                .invitedEmail(targetProfile.getEmail())
                .invitedDisplayName(resolveDisplayName(targetProfile))
                .invitedByUserId(actor.userId())
                .invitedByName(actor.displayName())
                .status(HouseholdInvitationStatus.PENDING)
                .build());

        createSystemMessage(householdId, actor.displayName() + " invited " + resolveDisplayName(targetProfile) + " to household");
        return householdInvitationMapper.toResponse(invitation, household.getName());
    }

    @Transactional(readOnly = true)
    public List<HouseholdInvitationResponse> getMyInvitations() {
        Actor actor = currentActor();
        return householdInvitationRepository.findAllByInvitedUserIdAndStatusOrderByCreatedAtDesc(
                        actor.userId(),
                        HouseholdInvitationStatus.PENDING
                ).stream()
                .map(invitation -> householdInvitationMapper.toResponse(
                        invitation,
                        findHousehold(invitation.getHouseholdId()).getName()
                ))
                .toList();
    }

    @Transactional
    public HouseholdResponse acceptInvitation(UUID invitationId) {
        Actor actor = currentActor();
        HouseholdInvitation invitation = householdInvitationRepository.findByIdAndInvitedUserId(invitationId, actor.userId())
                .orElseThrow(() -> new HouseholdInvitationNotFoundException(AppMessages.HOUSEHOLD_INVITATION_NOT_FOUND));

        if (invitation.getStatus() != HouseholdInvitationStatus.PENDING) {
            throw new ConflictException(AppMessages.HOUSEHOLD_INVITATION_IS_NOT_PENDING);
        }

        if (!householdMemberRepository.existsByHouseholdIdAndUserId(invitation.getHouseholdId(), actor.userId())) {
            householdMemberRepository.save(HouseholdMember.builder()
                    .householdId(invitation.getHouseholdId())
                    .userId(actor.userId())
                    .email(actor.email())
                    .displayName(actor.displayName())
                    .role(HouseholdRole.MEMBER)
                    .build());
        }

        invitation.setStatus(HouseholdInvitationStatus.ACCEPTED);
        invitation.setRespondedAt(java.time.LocalDateTime.now());
        householdInvitationRepository.save(invitation);

        createSystemMessage(invitation.getHouseholdId(), actor.displayName() + " accepted household invitation");
        return getHousehold(invitation.getHouseholdId());
    }

    @Transactional
    public HouseholdInvitationResponse declineInvitation(UUID invitationId) {
        Actor actor = currentActor();
        HouseholdInvitation invitation = householdInvitationRepository.findByIdAndInvitedUserId(invitationId, actor.userId())
                .orElseThrow(() -> new HouseholdInvitationNotFoundException(AppMessages.HOUSEHOLD_INVITATION_NOT_FOUND));

        if (invitation.getStatus() != HouseholdInvitationStatus.PENDING) {
            throw new ConflictException(AppMessages.HOUSEHOLD_INVITATION_IS_NOT_PENDING);
        }

        invitation.setStatus(HouseholdInvitationStatus.DECLINED);
        invitation.setRespondedAt(java.time.LocalDateTime.now());
        HouseholdInvitation saved = householdInvitationRepository.save(invitation);

        createSystemMessage(invitation.getHouseholdId(), actor.displayName() + " declined household invitation");
        return householdInvitationMapper.toResponse(saved, findHousehold(invitation.getHouseholdId()).getName());
    }

    @Transactional
    public void removeMember(UUID householdId, UUID memberUserId) {
        Actor actor = currentActor();
        Household household = findHousehold(householdId);
        ensureOwner(householdId, actor.userId());

        if (household.getCreatedByUserId().equals(memberUserId)) {
            throw new ForbiddenOperationException(AppMessages.OWNER_CANNOT_BE_REMOVED);
        }

        HouseholdMember member = householdMemberRepository.findByHouseholdIdAndUserId(householdId, memberUserId)
                .orElseThrow(() -> new HouseholdMemberNotFoundException(AppMessages.HOUSEHOLD_MEMBER_NOT_FOUND));
        householdMemberRepository.delete(member);
        createSystemMessage(householdId, actor.displayName() + " removed " + member.getDisplayName() + " from household");
    }

    @Transactional(readOnly = true)
    public List<HouseholdShoppingItemResponse> getShoppingItems(UUID householdId) {
        ensureCurrentUserMembership(householdId);
        return householdShoppingItemRepository.findAllByHouseholdIdOrderByCheckedAscCreatedAtDesc(householdId).stream()
                .map(householdShoppingItemMapper::toResponse)
                .toList();
    }

    @Transactional
    public HouseholdShoppingItemResponse addShoppingItem(UUID householdId, CreateHouseholdShoppingItemRequest request) {
        Actor actor = currentActor();
        ensureMembership(householdId, actor.userId());

        HouseholdShoppingItem item = householdShoppingItemMapper.toEntity(request);
        item.setHouseholdId(householdId);
        item.setChecked(false);
        item.setAddedByUserId(actor.userId());
        item.setAddedByName(actor.displayName());
        HouseholdShoppingItem saved = householdShoppingItemRepository.save(item);
        createSystemMessage(householdId, actor.displayName() + " added " + saved.getName() + " to shopping list");
        return householdShoppingItemMapper.toResponse(saved);
    }

    @Transactional
    public HouseholdShoppingItemResponse toggleShoppingItem(UUID householdId, UUID itemId) {
        Actor actor = currentActor();
        ensureMembership(householdId, actor.userId());
        HouseholdShoppingItem item = householdShoppingItemRepository.findByIdAndHouseholdId(itemId, householdId)
                .orElseThrow(() -> new HouseholdShoppingItemNotFoundException(AppMessages.HOUSEHOLD_SHOPPING_ITEM_NOT_FOUND));

        item.setChecked(!item.isChecked());
        if (item.isChecked()) {
            item.setCheckedByUserId(actor.userId());
            item.setCheckedByName(actor.displayName());
            createSystemMessage(householdId, actor.displayName() + " marked " + item.getName() + " as bought");
        } else {
            item.setCheckedByUserId(null);
            item.setCheckedByName(null);
            createSystemMessage(householdId, actor.displayName() + " unchecked " + item.getName());
        }
        return householdShoppingItemMapper.toResponse(householdShoppingItemRepository.save(item));
    }

    @Transactional
    public void deleteShoppingItem(UUID householdId, UUID itemId) {
        Actor actor = currentActor();
        ensureMembership(householdId, actor.userId());
        HouseholdShoppingItem item = householdShoppingItemRepository.findByIdAndHouseholdId(itemId, householdId)
                .orElseThrow(() -> new HouseholdShoppingItemNotFoundException(AppMessages.HOUSEHOLD_SHOPPING_ITEM_NOT_FOUND));
        householdShoppingItemRepository.delete(item);
        createSystemMessage(householdId, actor.displayName() + " removed " + item.getName() + " from shopping list");
    }

    @Transactional(readOnly = true)
    public List<HouseholdMessageResponse> getMessages(UUID householdId) {
        ensureCurrentUserMembership(householdId);
        return householdMessageRepository.findAllByHouseholdIdOrderByCreatedAtDesc(householdId).stream()
                .map(householdMessageMapper::toResponse)
                .toList();
    }

    @Transactional
    public HouseholdMessageResponse createMessage(UUID householdId, CreateHouseholdMessageRequest request) {
        Actor actor = currentActor();
        ensureMembership(householdId, actor.userId());
        HouseholdMessage message = householdMessageRepository.save(HouseholdMessage.builder()
                .householdId(householdId)
                .authorUserId(actor.userId())
                .authorName(actor.displayName())
                .message(request.getMessage().trim())
                .type(HouseholdMessageType.USER)
                .build());
        return householdMessageMapper.toResponse(message);
    }

    private void ensureCurrentUserMembership(UUID householdId) {
        Actor actor = currentActor();
        ensureMembership(householdId, actor.userId());
    }

    private void ensureMembership(UUID householdId, UUID userId) {
        if (householdMemberRepository.findByHouseholdIdAndUserId(householdId, userId).isEmpty()) {
            throw new HouseholdNotFoundException(AppMessages.HOUSEHOLD_NOT_FOUND);
        }
    }

    private void ensureOwner(UUID householdId, UUID userId) {
        HouseholdMember member = householdMemberRepository.findByHouseholdIdAndUserId(householdId, userId)
                .orElseThrow(() -> new HouseholdNotFoundException(AppMessages.HOUSEHOLD_NOT_FOUND));
        if (member.getRole() != HouseholdRole.OWNER) {
            throw new ForbiddenOperationException(AppMessages.ONLY_OWNER_CAN_MANAGE_MEMBERS);
        }
    }

    private Household findHousehold(UUID householdId) {
        return householdRepository.findById(householdId)
                .orElseThrow(() -> new HouseholdNotFoundException(AppMessages.HOUSEHOLD_NOT_FOUND));
    }

    private Actor currentActor() {
        UserProfileResponse profile = fetchProfile(SecurityUtils.getCurrentUsername());
        return new Actor(profile.getId(), resolveDisplayName(profile), profile.getEmail());
    }

    private UserProfileResponse fetchProfile(String email) {
        try {
            return userProfileClient.getProfile(email);
        } catch (Exception exception) {
            throw new UpstreamServiceException(AppMessages.FAILED_TO_FETCH_PROFILE, exception);
        }
    }

    private String resolveDisplayName(UserProfileResponse profile) {
        if (profile.getName() != null && !profile.getName().isBlank()) {
            return profile.getName();
        }
        return profile.getEmail();
    }

    private void createSystemMessage(UUID householdId, String message) {
        householdMessageRepository.save(HouseholdMessage.builder()
                .householdId(householdId)
                .authorUserId(null)
                .authorName("system")
                .message(message)
                .type(HouseholdMessageType.SYSTEM)
                .build());
    }

    private record Actor(UUID userId, String displayName, String email) {
    }
}
