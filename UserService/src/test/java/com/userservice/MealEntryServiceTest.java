package com.userservice;

import com.userservice.meals.dto.MealEntryResponse;
import com.userservice.meals.dto.MealListItemResponse;
import com.userservice.meals.mapper.MealEntryMapper;
import com.userservice.meals.model.MealEntry;
import com.userservice.meals.model.enums.MealSource;
import com.userservice.meals.repository.MealEntryRepository;
import com.userservice.meals.service.MealEntryService;
import com.userservice.profile.repository.UserProfileRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MealEntryServiceTest {

    @Mock
    private MealEntryRepository mealEntryRepository;
    @Mock
    private UserProfileRepository userProfileRepository;

    private MealEntryService mealEntryService;

    @BeforeEach
    void setUp() {
        MealEntryMapper mapper = Mappers.getMapper(MealEntryMapper.class);
        mealEntryService = new MealEntryService(
                mealEntryRepository,
                mapper,
                userProfileRepository
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
    void createMealShouldAssignCurrentUserAndDefaultSource() {
        var request = TestDataFactory.mealCreateRequest();
        request.setSource(null);

        when(userProfileRepository.findByEmail(TestDataFactory.EMAIL)).thenReturn(Optional.of(TestDataFactory.profile()));
        when(mealEntryRepository.save(any(MealEntry.class))).thenAnswer(invocation -> {
            MealEntry entry = invocation.getArgument(0);
            entry.setId(TestDataFactory.MEAL_ENTRY_ID);
            return entry;
        });

        MealEntryResponse response = mealEntryService.createMeal(request);

        assertEquals(TestDataFactory.MEAL_ENTRY_ID, response.getId());
        assertEquals(MealSource.MANUAL, response.getSource());
    }

    @Test
    void getMealsShouldReturnFilteredEntriesWhenDatesProvided() {
        when(userProfileRepository.findByEmail(TestDataFactory.EMAIL)).thenReturn(Optional.of(TestDataFactory.profile()));
        when(mealEntryRepository.findAllByUserIdAndEatenAtBetweenOrderByEatenAtDesc(
                any(),
                any(),
                any()
        )).thenReturn(List.of(TestDataFactory.mealEntry()));

        List<MealListItemResponse> responses = mealEntryService.getMeals(
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31)
        );

        assertEquals(1, responses.size());
        assertEquals("Chicken salad", responses.get(0).getTitle());
    }

    @Test
    void getMealByIdShouldReturnOwnedMeal() {
        when(userProfileRepository.findByEmail(TestDataFactory.EMAIL)).thenReturn(Optional.of(TestDataFactory.profile()));
        when(mealEntryRepository.findByIdAndUserId(TestDataFactory.MEAL_ENTRY_ID, TestDataFactory.USER_ID))
                .thenReturn(Optional.of(TestDataFactory.mealEntry()));

        MealEntryResponse response = mealEntryService.getMealById(TestDataFactory.MEAL_ENTRY_ID);

        assertEquals("Chicken salad", response.getTitle());
    }

    @Test
    void deleteMealShouldDeleteOwnedEntry() {
        when(userProfileRepository.findByEmail(TestDataFactory.EMAIL)).thenReturn(Optional.of(TestDataFactory.profile()));
        when(mealEntryRepository.findByIdAndUserId(TestDataFactory.MEAL_ENTRY_ID, TestDataFactory.USER_ID))
                .thenReturn(Optional.of(TestDataFactory.mealEntry()));

        mealEntryService.deleteMeal(TestDataFactory.MEAL_ENTRY_ID);

        verify(mealEntryRepository).delete(any(MealEntry.class));
    }
}
