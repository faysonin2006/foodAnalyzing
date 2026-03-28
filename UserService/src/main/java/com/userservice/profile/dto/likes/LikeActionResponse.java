package com.userservice.profile.dto.likes;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LikeActionResponse {
    private Long recipeId;
    private boolean liked;
    private boolean changed;
    private LocalDateTime createdAt;
}
