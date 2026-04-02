package com.userservice;

import com.userservice.pantry.dto.PantryItemResponse;
import com.userservice.pantry.dto.PantryListItemResponse;
import com.userservice.pantry.mapper.PantryItemMapper;
import com.userservice.pantry.model.PantryItem;
import com.userservice.pantry.model.enums.PantryItemStatus;
import com.userservice.pantry.repository.PantryItemRepository;
import com.userservice.pantry.service.PantryImageStorageService;
import com.userservice.pantry.service.PantryItemService;
import com.userservice.profile.mapper.UserProfileMapper;
import com.userservice.profile.repository.UserAllergyRepository;
import com.userservice.profile.repository.UserDietRepository;
import com.userservice.profile.repository.UserHealthConditionRepository;
import com.userservice.profile.repository.UserLikesRepository;
import com.userservice.profile.repository.UserProfileRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PantryItemServiceTest {

    @Mock
    private PantryItemRepository pantryItemRepository;
    @Mock
    private UserProfileRepository userProfileRepository;
    @Mock
    private PantryImageStorageService pantryImageStorageService;

    private PantryItemService pantryItemService;

    @BeforeEach
    void setUp() {
        PantryItemMapper mapper = Mappers.getMapper(PantryItemMapper.class);
        pantryItemService = new PantryItemService(
                pantryItemRepository,
                mapper,
                userProfileRepository,
                pantryImageStorageService
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
    void createPantryItemShouldAssignCurrentUserAndActiveStatus() {
        when(userProfileRepository.findByEmail(TestDataFactory.EMAIL)).thenReturn(Optional.of(TestDataFactory.profile()));
        when(pantryItemRepository.save(any(PantryItem.class))).thenAnswer(invocation -> {
            PantryItem item = invocation.getArgument(0);
            item.setId(TestDataFactory.PANTRY_ITEM_ID);
            return item;
        });

        PantryItemResponse response = pantryItemService.createPantryItem(TestDataFactory.pantryCreateRequest());

        ArgumentCaptor<PantryItem> captor = ArgumentCaptor.forClass(PantryItem.class);
        verify(pantryItemRepository).save(captor.capture());
        PantryItem saved = captor.getValue();

        assertEquals(TestDataFactory.USER_ID, saved.getUserId());
        assertEquals(PantryItemStatus.ACTIVE, saved.getStatus());
        assertEquals("Milk", response.getName());
        assertEquals(PantryItemStatus.ACTIVE, response.getStatus());
    }

    @Test
    void uploadPantryItemImageShouldPersistImageUrl() throws Exception {
        PantryItem pantryItem = TestDataFactory.pantryItem();
        MockMultipartFile file = new MockMultipartFile("file", "milk.png", "image/png", "png".getBytes());

        when(userProfileRepository.findByEmail(TestDataFactory.EMAIL)).thenReturn(Optional.of(TestDataFactory.profile()));
        when(pantryItemRepository.findByIdAndUserId(TestDataFactory.PANTRY_ITEM_ID, TestDataFactory.USER_ID))
                .thenReturn(Optional.of(pantryItem));
        when(pantryImageStorageService.uploadImage(file, TestDataFactory.USER_ID, TestDataFactory.PANTRY_ITEM_ID))
                .thenReturn("https://cdn.example.com/pantry/milk.png");
        when(pantryItemRepository.save(any(PantryItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PantryItemResponse response = pantryItemService.uploadPantryItemImage(TestDataFactory.PANTRY_ITEM_ID, file);

        assertEquals("https://cdn.example.com/pantry/milk.png", response.getImageUrl());
    }

    @Test
    void getExpiredItemsShouldMarkActiveExpiredItemsAndReturnThem() {
        PantryItem expiredItem = TestDataFactory.pantryItem();
        expiredItem.setExpiresAt(LocalDate.now().minusDays(1));
        expiredItem.setStatus(PantryItemStatus.ACTIVE);

        when(userProfileRepository.findByEmail(TestDataFactory.EMAIL)).thenReturn(Optional.of(TestDataFactory.profile()));
        when(pantryItemRepository.findAllByUserIdAndStatusAndExpiresAtBefore(
                TestDataFactory.USER_ID,
                PantryItemStatus.ACTIVE,
                LocalDate.now()
        )).thenReturn(List.of(expiredItem));
        when(pantryItemRepository.findAllByUserIdAndStatus(TestDataFactory.USER_ID, PantryItemStatus.EXPIRED))
                .thenReturn(List.of(expiredItem));

        List<PantryListItemResponse> responses = pantryItemService.getExpiredItems();

        assertEquals(1, responses.size());
        assertEquals(PantryItemStatus.EXPIRED, expiredItem.getStatus());
        assertEquals(PantryItemStatus.EXPIRED, responses.get(0).getStatus());
        verify(pantryItemRepository).saveAll(List.of(expiredItem));
    }

    @Test
    void deletePantryItemShouldSoftDeleteItem() {
        PantryItem pantryItem = TestDataFactory.pantryItem();

        when(userProfileRepository.findByEmail(TestDataFactory.EMAIL)).thenReturn(Optional.of(TestDataFactory.profile()));
        when(pantryItemRepository.findByIdAndUserId(TestDataFactory.PANTRY_ITEM_ID, TestDataFactory.USER_ID))
                .thenReturn(Optional.of(pantryItem));
        when(pantryItemRepository.save(any(PantryItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        pantryItemService.deletePantryItem(TestDataFactory.PANTRY_ITEM_ID);

        assertNotNull(pantryItem);
        assertEquals(PantryItemStatus.REMOVED, pantryItem.getStatus());
    }
}
