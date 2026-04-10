package recipes.recipesfromdbservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import recipes.recipesfromdbservice.models.RecipeCommentLike;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RecipeCommentLikeRepository extends JpaRepository<RecipeCommentLike, Long> {
    Optional<RecipeCommentLike> findByCommentIdAndUserId(Long commentId, UUID userId);

    List<RecipeCommentLike> findByCommentIdIn(Collection<Long> commentIds);
}
