package recipes.recipesfromdbservice.dtos.responseDtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecipeCommentDto {
    private Long id;
    private Long recipeId;
    private Long parentCommentId;
    private String authorName;
    private String body;
    private Instant createdAt;
    private int likeCount;
    private boolean likedByMe;
    private int replyCount;
    private List<RecipeCommentDto> replies;
}
