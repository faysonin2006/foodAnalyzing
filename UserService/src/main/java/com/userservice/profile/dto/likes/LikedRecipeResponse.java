package com.userservice.profile.dto.likes;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LikedRecipeResponse {
    private Long recipeId;
    private LocalDateTime createdAt;
}
