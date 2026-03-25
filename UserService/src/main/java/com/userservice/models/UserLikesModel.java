package com.userservice.models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "user_likes",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_likes_user_recipe",
                columnNames = {"user_id", "recipe_id"}
        )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserLikesModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "recipe_id", nullable = false)
    private Long recipeId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
