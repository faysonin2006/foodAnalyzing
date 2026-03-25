package com.userservice;

import com.userservice.dtos.UserProfileResponse;
import com.userservice.dtos.likes.LikeActionResponse;
import com.userservice.exceptions.BadRequestException;
import com.userservice.mappers.UserProfileMapper;
import com.userservice.models.UserProfile;
import com.userservice.repositories.UserAllergyRepository;
import com.userservice.repositories.UserDietRepository;
import com.userservice.repositories.UserHealthConditionRepository;
import com.userservice.repositories.UserLikesRepository;
import com.userservice.repositories.UserProfileRepository;
import com.userservice.services.UserService;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserProfileRepository profileRepository;
    @Mock
    private UserAllergyRepository allergyRepository;
    @Mock
    private UserDietRepository dietRepository;
    @Mock
    private UserHealthConditionRepository healthConditionRepository;
    @Mock
    private UserLikesRepository likesRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        UserProfileMapper mapper = Mappers.getMapper(UserProfileMapper.class);
        userService = new UserService(
                profileRepository,
                allergyRepository,
                dietRepository,
                healthConditionRepository,
                likesRepository,
                mapper
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
    void getCurrentProfileShouldReturnMappedProfile() {
        UserProfile profile = TestDataFactory.profile();
        when(profileRepository.findByEmail(TestDataFactory.EMAIL)).thenReturn(Optional.of(profile));

        UserProfileResponse response = userService.getCurrentProfile();

        assertEquals(profile.getEmail(), response.getEmail());
        assertEquals(profile.getTargetCaloriesPerDay(), response.getTargetCalories());
    }

    @Test
    void updateCurrentProfileShouldApplyRelationsAndRecalculateCalories() {
        UserProfile profile = TestDataFactory.profile();
        when(profileRepository.findByEmail(TestDataFactory.EMAIL)).thenReturn(Optional.of(profile));
        when(allergyRepository.findAllById(TestDataFactory.updateRequest().getAllergies())).thenReturn(List.of(TestDataFactory.allergy()));
        when(profileRepository.save(profile)).thenReturn(profile);

        UserProfileResponse response = userService.updateCurrentProfile(TestDataFactory.updateRequest());

        assertEquals("Alex Updated", response.getName());
        assertEquals(1, profile.getAllergies().size());
        assertNotNull(response.getTargetCalories());
    }

    @Test
    void createLikeShouldCreateLikeForCurrentUser() {
        UserProfile profile = TestDataFactory.profile();
        var row = TestDataFactory.like();
        when(profileRepository.findByEmail(TestDataFactory.EMAIL)).thenReturn(Optional.of(profile));
        when(likesRepository.insertIgnore(any(UUID.class), eq(TestDataFactory.USER_ID), eq(42L))).thenReturn(1);
        when(likesRepository.findByUserIdAndRecipeId(TestDataFactory.USER_ID, 42L)).thenReturn(Optional.of(row));

        LikeActionResponse response = userService.createLike(42L);

        assertEquals(LikeActionResponse.builder()
                .recipeId(42L)
                .liked(true)
                .changed(true)
                .createdAt(row.getCreatedAt())
                .build(), response);
    }

    @Test
    void createLikeShouldRejectInvalidRecipeId() {
        assertThrows(BadRequestException.class, () -> userService.createLike(0L));
        verifyNoInteractions(likesRepository);
    }
}
