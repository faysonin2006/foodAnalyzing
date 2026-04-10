package recipes.recipesfromdbservice.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "recipe_comments", schema = "cookbook_wh")
public class RecipeComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recipe_id", nullable = false)
    private Long recipeId;

    @Column(name = "parent_comment_id")
    private Long parentCommentId;

    @Column(name = "author_user_id")
    private UUID authorUserId;

    @Column(name = "author_email", nullable = false, length = 255)
    private String authorEmail;

    @Column(name = "author_name", nullable = false, length = 120)
    private String authorName;

    @Column(name = "body", nullable = false, length = 1000)
    private String body;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
