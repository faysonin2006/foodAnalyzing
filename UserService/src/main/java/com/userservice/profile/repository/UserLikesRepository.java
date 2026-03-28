package com.userservice.profile.repository;

import com.userservice.profile.model.UserLikesModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface UserLikesRepository extends JpaRepository<UserLikesModel, UUID> {

    Optional<UserLikesModel> findByUserIdAndRecipeId(UUID userId, Long recipeId);

    @Modifying
    @Query(value = """
        insert into user_likes (id, user_id, recipe_id, created_at)
        values (:id, :userId, :recipeId, now())
        on conflict (user_id, recipe_id) do nothing
        """, nativeQuery = true)
    int insertIgnore(@Param("id") UUID id,
                     @Param("userId") UUID userId,
                     @Param("recipeId") Long recipeId);

    int deleteByUserIdAndRecipeId(UUID userId, Long recipeId);

    List<UserLikesModel> findAllByUserIdOrderByCreatedAtDesc(UUID userId);
}

