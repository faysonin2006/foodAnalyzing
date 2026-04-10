package recipes.recipesfromdbservice.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateRecipeCommentRequest {

    @NotBlank
    @Size(max = 1000)
    private String text;

    private Long parentCommentId;
}
