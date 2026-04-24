package recipes.recipesfromdbservice.dtos;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ProductSearchPageResponse {
    private List<ProductCardResponse> items;
    private Integer page;
    private Integer size;
    private Boolean hasNext;
}
