package recipes.recipesfromdbservice.repositories.projections;

import java.math.BigDecimal;

public interface ConstraintRow {
    String getKey();
    String getType();
    String getStatus();
    String getReason();
    String getSource();
    BigDecimal getConfidence();
}
