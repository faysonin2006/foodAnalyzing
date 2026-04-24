package recipes.recipesfromdbservice.repositories.projections;

public interface InstructionRow {
    Integer getPosition();
    String getText();
    String getDurationHint();
    String getTemperatureHint();
}
