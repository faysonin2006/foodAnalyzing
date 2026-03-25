package recipes.recipesfromdbservice.dtos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Languages {
    EN("en"),
    RU("ru");

    private final String lowerCaseString;

    Languages(String lowerCaseString) {
        this.lowerCaseString = lowerCaseString;
    }

    @JsonValue
    public String getLowerCaseString() {
        return lowerCaseString;
    }

    @JsonCreator
    public static Languages fromValue(String value) {
        if (value == null) return null;
        return switch (value.trim().toLowerCase()) {
            case "en" -> EN;
            case "ru" -> RU;
            default -> throw new IllegalArgumentException("Unsupported language: " + value);
        };
    }
}

