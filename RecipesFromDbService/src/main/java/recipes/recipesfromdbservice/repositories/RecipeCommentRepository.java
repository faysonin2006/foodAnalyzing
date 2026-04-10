package recipes.recipesfromdbservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import recipes.recipesfromdbservice.models.RecipeComment;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecipeCommentRepository extends JpaRepository<RecipeComment, Long> {
    List<RecipeComment> findByRecipeIdOrderByCreatedAtAscIdAsc(Long recipeId);

    Optional<RecipeComment> findByIdAndRecipeId(Long id, Long recipeId);
}
