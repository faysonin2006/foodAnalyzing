package com.userservice;

import com.userservice.profile.repository.UserProfileRepository;
import com.userservice.shopping.dto.CreateShoppingListItemRequest;
import com.userservice.shopping.dto.ShoppingListItemResponse;
import com.userservice.shopping.mapper.ShoppingListItemMapper;
import com.userservice.shopping.model.ShoppingListItem;
import com.userservice.shopping.repository.ShoppingListItemRepository;
import com.userservice.shopping.service.ShoppingListService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShoppingListServiceTest {

    @Mock
    private ShoppingListItemRepository shoppingListItemRepository;
    @Mock
    private UserProfileRepository userProfileRepository;

    private ShoppingListService shoppingListService;

    @BeforeEach
    void setUp() {
        ShoppingListItemMapper mapper = Mappers.getMapper(ShoppingListItemMapper.class);
        shoppingListService = new ShoppingListService(shoppingListItemRepository, mapper, userProfileRepository);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(TestDataFactory.EMAIL, null, List.of())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createItemShouldCreateUncheckedShoppingItem() {
        when(userProfileRepository.findByEmail(TestDataFactory.EMAIL)).thenReturn(Optional.of(TestDataFactory.profile()));
        when(shoppingListItemRepository.save(any(ShoppingListItem.class))).thenAnswer(invocation -> {
            ShoppingListItem item = invocation.getArgument(0);
            item.setId(TestDataFactory.SHOPPING_ITEM_ID);
            return item;
        });

        ShoppingListItemResponse response = shoppingListService.createItem(TestDataFactory.shoppingCreateRequest());

        assertEquals(TestDataFactory.SHOPPING_ITEM_ID, response.getId());
        assertEquals("Tomatoes", response.getName());
    }

    @Test
    void toggleCheckedShouldFlipFlag() {
        ShoppingListItem item = TestDataFactory.shoppingItem();
        when(userProfileRepository.findByEmail(TestDataFactory.EMAIL)).thenReturn(Optional.of(TestDataFactory.profile()));
        when(shoppingListItemRepository.findByIdAndUserId(TestDataFactory.SHOPPING_ITEM_ID, TestDataFactory.USER_ID))
                .thenReturn(Optional.of(item));
        when(shoppingListItemRepository.save(any(ShoppingListItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShoppingListItemResponse response = shoppingListService.toggleChecked(TestDataFactory.SHOPPING_ITEM_ID);

        assertEquals(true, response.isChecked());
    }

    @Test
    void getItemsShouldReturnUserShoppingItems() {
        when(userProfileRepository.findByEmail(TestDataFactory.EMAIL)).thenReturn(Optional.of(TestDataFactory.profile()));
        when(shoppingListItemRepository.findAllByUserIdOrderByCheckedAscCreatedAtDesc(TestDataFactory.USER_ID))
                .thenReturn(List.of(TestDataFactory.shoppingItem()));

        List<ShoppingListItemResponse> responses = shoppingListService.getItems();

        assertEquals(1, responses.size());
        assertEquals("Tomatoes", responses.get(0).getName());
    }

    @Test
    void createItemsForEmailShouldCreateAllItemsForGivenUser() {
        when(userProfileRepository.findByEmail(TestDataFactory.EMAIL)).thenReturn(Optional.of(TestDataFactory.profile()));
        when(shoppingListItemRepository.save(any(ShoppingListItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<ShoppingListItemResponse> responses = shoppingListService.createItemsForEmail(
                TestDataFactory.EMAIL,
                List.of(
                        TestDataFactory.shoppingCreateRequest(),
                        CreateShoppingListItemRequest.builder().name("Lettuce").build()
                )
        );

        assertEquals(2, responses.size());
        assertEquals("Tomatoes", responses.get(0).getName());
        assertEquals("Lettuce", responses.get(1).getName());
    }
}
