package recipes.householdservice;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import recipes.householdservice.clients.UserProfileClient;
import recipes.householdservice.dto.household.HouseholdInvitationResponse;
import recipes.householdservice.dto.household.HouseholdMemberResponse;
import recipes.householdservice.dto.household.HouseholdMessageResponse;
import recipes.householdservice.dto.household.HouseholdResponse;
import recipes.householdservice.dto.household.HouseholdShoppingItemResponse;
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
import recipes.householdservice.household.repository.HouseholdInvitationRepository;
import recipes.householdservice.household.repository.HouseholdMemberRepository;
import recipes.householdservice.household.repository.HouseholdMessageRepository;
import recipes.householdservice.household.repository.HouseholdRepository;
import recipes.householdservice.household.repository.HouseholdShoppingItemRepository;
import recipes.householdservice.household.service.HouseholdService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HouseholdServiceTest {

    @Mock
    private HouseholdRepository householdRepository;
    @Mock
    private HouseholdMemberRepository householdMemberRepository;
    @Mock
    private HouseholdInvitationRepository householdInvitationRepository;
    @Mock
    private HouseholdShoppingItemRepository householdShoppingItemRepository;
    @Mock
    private HouseholdMessageRepository householdMessageRepository;
    @Mock
    private UserProfileClient userProfileClient;

    private HouseholdService householdService;

    @BeforeEach
    void setUp() {
        householdService = new HouseholdService(
                householdRepository,
                householdMemberRepository,
                householdInvitationRepository,
                householdShoppingItemRepository,
                householdMessageRepository,
                new HouseholdMapper(),
                new HouseholdInvitationMapper(),
                Mappers.getMapper(HouseholdMemberMapper.class),
                Mappers.getMapper(HouseholdShoppingItemMapper.class),
                Mappers.getMapper(HouseholdMessageMapper.class),
                userProfileClient
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(TestDataFactory.EMAIL, null, List.of())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createHouseholdShouldCreateOwnerMember() {
        Household household = TestDataFactory.household();
        HouseholdMember ownerMember = TestDataFactory.ownerMember();
        when(userProfileClient.getProfile(TestDataFactory.EMAIL)).thenReturn(TestDataFactory.ownerProfile());
        when(householdRepository.save(any(Household.class))).thenReturn(household);
        when(householdMemberRepository.save(any(HouseholdMember.class))).thenReturn(ownerMember);
        when(householdMemberRepository.findByHouseholdIdAndUserId(TestDataFactory.HOUSEHOLD_ID, TestDataFactory.USER_ID))
                .thenReturn(Optional.of(ownerMember));
        when(householdRepository.findById(TestDataFactory.HOUSEHOLD_ID)).thenReturn(Optional.of(household));
        when(householdMemberRepository.findAllByHouseholdIdOrderByJoinedAtAsc(TestDataFactory.HOUSEHOLD_ID))
                .thenReturn(List.of(ownerMember));
        when(householdShoppingItemRepository.countByHouseholdIdAndCheckedFalse(TestDataFactory.HOUSEHOLD_ID)).thenReturn(0L);
        when(householdMessageRepository.save(any(HouseholdMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        HouseholdResponse response = householdService.createHousehold(TestDataFactory.createHouseholdRequest());

        assertEquals("Family", response.getName());
        assertEquals(1, response.getMembers().size());
        assertEquals("Owner", response.getMembers().get(0).getDisplayName());
    }

    @Test
    void addMemberShouldReturnCreatedMember() {
        when(userProfileClient.getProfile(TestDataFactory.EMAIL)).thenReturn(TestDataFactory.ownerProfile());
        when(userProfileClient.getProfile(TestDataFactory.MEMBER_EMAIL)).thenReturn(TestDataFactory.memberProfile());
        when(householdMemberRepository.findByHouseholdIdAndUserId(TestDataFactory.HOUSEHOLD_ID, TestDataFactory.USER_ID))
                .thenReturn(Optional.of(TestDataFactory.ownerMember()));
        when(householdMemberRepository.existsByHouseholdIdAndUserId(TestDataFactory.HOUSEHOLD_ID, TestDataFactory.MEMBER_ID))
                .thenReturn(false);
        when(householdMemberRepository.save(any(HouseholdMember.class))).thenReturn(TestDataFactory.member());
        when(householdMessageRepository.save(any(HouseholdMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        HouseholdMemberResponse response = householdService.addMember(TestDataFactory.HOUSEHOLD_ID, TestDataFactory.addMemberRequest());

        assertEquals("Member", response.getDisplayName());
        assertEquals(TestDataFactory.MEMBER_EMAIL, response.getEmail());
    }

    @Test
    void createInvitationShouldReturnPendingInvitation() {
        when(userProfileClient.getProfile(TestDataFactory.EMAIL)).thenReturn(TestDataFactory.ownerProfile());
        when(userProfileClient.getProfile(TestDataFactory.MEMBER_EMAIL)).thenReturn(TestDataFactory.memberProfile());
        when(householdMemberRepository.findByHouseholdIdAndUserId(TestDataFactory.HOUSEHOLD_ID, TestDataFactory.USER_ID))
                .thenReturn(Optional.of(TestDataFactory.ownerMember()));
        when(householdMemberRepository.existsByHouseholdIdAndUserId(TestDataFactory.HOUSEHOLD_ID, TestDataFactory.MEMBER_ID))
                .thenReturn(false);
        when(householdInvitationRepository.existsByHouseholdIdAndInvitedUserIdAndStatus(
                TestDataFactory.HOUSEHOLD_ID,
                TestDataFactory.MEMBER_ID,
                HouseholdInvitationStatus.PENDING
        )).thenReturn(false);
        when(householdRepository.findById(TestDataFactory.HOUSEHOLD_ID)).thenReturn(Optional.of(TestDataFactory.household()));
        when(householdInvitationRepository.save(any(HouseholdInvitation.class))).thenReturn(TestDataFactory.invitation());
        when(householdMessageRepository.save(any(HouseholdMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        HouseholdInvitationResponse response = householdService.createInvitation(
                TestDataFactory.HOUSEHOLD_ID,
                TestDataFactory.createInvitationRequest()
        );

        assertEquals("PENDING", response.getStatus());
        assertEquals("Member", response.getInvitedDisplayName());
        assertEquals("Family", response.getHouseholdName());
    }

    @Test
    void acceptInvitationShouldCreateMembershipAndReturnHousehold() {
        HouseholdInvitation invitation = TestDataFactory.invitation();
        when(userProfileClient.getProfile(TestDataFactory.MEMBER_EMAIL)).thenReturn(TestDataFactory.memberProfile());
        when(householdInvitationRepository.findByIdAndInvitedUserId(TestDataFactory.INVITATION_ID, TestDataFactory.MEMBER_ID))
                .thenReturn(Optional.of(invitation));
        when(householdMemberRepository.existsByHouseholdIdAndUserId(TestDataFactory.HOUSEHOLD_ID, TestDataFactory.MEMBER_ID))
                .thenReturn(false);
        when(householdMemberRepository.save(any(HouseholdMember.class))).thenReturn(TestDataFactory.member());
        when(householdInvitationRepository.save(any(HouseholdInvitation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(householdMessageRepository.save(any(HouseholdMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(householdMemberRepository.findByHouseholdIdAndUserId(TestDataFactory.HOUSEHOLD_ID, TestDataFactory.MEMBER_ID))
                .thenReturn(Optional.of(TestDataFactory.member()));
        when(householdRepository.findById(TestDataFactory.HOUSEHOLD_ID)).thenReturn(Optional.of(TestDataFactory.household()));
        when(householdMemberRepository.findAllByHouseholdIdOrderByJoinedAtAsc(TestDataFactory.HOUSEHOLD_ID))
                .thenReturn(List.of(TestDataFactory.ownerMember(), TestDataFactory.member()));
        when(householdShoppingItemRepository.countByHouseholdIdAndCheckedFalse(TestDataFactory.HOUSEHOLD_ID)).thenReturn(0L);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(TestDataFactory.MEMBER_EMAIL, null, List.of())
        );

        HouseholdResponse response = householdService.acceptInvitation(TestDataFactory.INVITATION_ID);

        assertEquals("Family", response.getName());
        assertEquals(2, response.getMembers().size());
        assertEquals(HouseholdInvitationStatus.ACCEPTED, invitation.getStatus());
    }

    @Test
    void getMyInvitationsShouldReturnPendingInvitations() {
        when(userProfileClient.getProfile(TestDataFactory.MEMBER_EMAIL)).thenReturn(TestDataFactory.memberProfile());
        when(householdInvitationRepository.findAllByInvitedUserIdAndStatusOrderByCreatedAtDesc(
                TestDataFactory.MEMBER_ID,
                HouseholdInvitationStatus.PENDING
        )).thenReturn(List.of(TestDataFactory.invitation()));
        when(householdRepository.findById(TestDataFactory.HOUSEHOLD_ID)).thenReturn(Optional.of(TestDataFactory.household()));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(TestDataFactory.MEMBER_EMAIL, null, List.of())
        );

        List<HouseholdInvitationResponse> responses = householdService.getMyInvitations();

        assertEquals(1, responses.size());
        assertEquals("PENDING", responses.get(0).getStatus());
        assertEquals("Family", responses.get(0).getHouseholdName());
    }

    @Test
    void declineInvitationShouldUpdateStatus() {
        HouseholdInvitation invitation = TestDataFactory.invitation();
        when(userProfileClient.getProfile(TestDataFactory.MEMBER_EMAIL)).thenReturn(TestDataFactory.memberProfile());
        when(householdInvitationRepository.findByIdAndInvitedUserId(TestDataFactory.INVITATION_ID, TestDataFactory.MEMBER_ID))
                .thenReturn(Optional.of(invitation));
        when(householdInvitationRepository.save(any(HouseholdInvitation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(householdRepository.findById(TestDataFactory.HOUSEHOLD_ID)).thenReturn(Optional.of(TestDataFactory.household()));
        when(householdMessageRepository.save(any(HouseholdMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(TestDataFactory.MEMBER_EMAIL, null, List.of())
        );

        HouseholdInvitationResponse response = householdService.declineInvitation(TestDataFactory.INVITATION_ID);

        assertEquals("DECLINED", response.getStatus());
        assertEquals(HouseholdInvitationStatus.DECLINED, invitation.getStatus());
    }

    @Test
    void addShoppingItemShouldCreateUncheckedItem() {
        when(userProfileClient.getProfile(TestDataFactory.EMAIL)).thenReturn(TestDataFactory.ownerProfile());
        when(householdMemberRepository.findByHouseholdIdAndUserId(TestDataFactory.HOUSEHOLD_ID, TestDataFactory.USER_ID))
                .thenReturn(Optional.of(TestDataFactory.ownerMember()));
        when(householdShoppingItemRepository.save(any(HouseholdShoppingItem.class))).thenReturn(TestDataFactory.shoppingItem());
        when(householdMessageRepository.save(any(HouseholdMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        HouseholdShoppingItemResponse response = householdService.addShoppingItem(
                TestDataFactory.HOUSEHOLD_ID,
                TestDataFactory.createShoppingItemRequest()
        );

        assertEquals("Milk", response.getName());
        assertEquals(false, response.isChecked());
    }

    @Test
    void createMessageShouldReturnUserMessage() {
        when(userProfileClient.getProfile(TestDataFactory.EMAIL)).thenReturn(TestDataFactory.ownerProfile());
        when(householdMemberRepository.findByHouseholdIdAndUserId(TestDataFactory.HOUSEHOLD_ID, TestDataFactory.USER_ID))
                .thenReturn(Optional.of(TestDataFactory.ownerMember()));
        when(householdMessageRepository.save(any(HouseholdMessage.class))).thenReturn(TestDataFactory.message());

        HouseholdMessageResponse response = householdService.createMessage(
                TestDataFactory.HOUSEHOLD_ID,
                TestDataFactory.createMessageRequest()
        );

        assertEquals("Buy bread too", response.getMessage());
        assertEquals("Owner", response.getAuthorName());
    }
}
