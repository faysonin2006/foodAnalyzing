package com.recipeservice.constants;

public final class AppMessages {

    public static final String SEARCH_REQUEST_MUST_NOT_BE_NULL = "Recipe search request must not be null";
    public static final String RECOMMENDATION_REQUEST_MUST_NOT_BE_NULL = "Recipe recommendation request must not be null";
    public static final String RECIPE_ID_MUST_NOT_BE_BLANK = "Recipe id must not be blank";
    public static final String INVALID_RECIPE_ID = "Recipe id must be positive";
    public static final String FAILED_TO_FETCH_RECIPES = "Failed to fetch recipes from Spoonacular";
    public static final String FAILED_TO_FETCH_RECIPES_DB = "Failed to fetch recipes from recipe database";
    public static final String FAILED_TO_FETCH_INSTRUCTIONS = "Failed to fetch recipe instructions from Spoonacular";
    public static final String FAILED_TO_ADD_MISSING_INGREDIENTS = "Failed to add missing ingredients to shopping list";
    public static final String FAILED_TO_FETCH_PROFILE = "Failed to fetch user profile";
    public static final String FAILED_TO_FETCH_PANTRY = "Failed to fetch pantry items";
    public static final String AUTHENTICATION_REQUIRED = "Authentication is required";

    private AppMessages() {
    }
}
