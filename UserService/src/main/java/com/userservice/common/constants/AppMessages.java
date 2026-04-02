package com.userservice.common.constants;

import lombok.experimental.UtilityClass;

@UtilityClass
public class AppMessages {

    public static final String PROFILE_NOT_FOUND = "Profile not found";
    public static final String PANTRY_ITEM_NOT_FOUND = "Pantry item not found";
    public static final String MEAL_ENTRY_NOT_FOUND = "Meal entry not found";
    public static final String SHOPPING_LIST_ITEM_NOT_FOUND = "Shopping list item not found";
    public static final String INVALID_RECIPE_ID = "Invalid recipeId";
    public static final String LIKE_ROW_MISSING = "Like row missing";
    public static final String FILE_MUST_NOT_BE_EMPTY = "File must not be empty";
    public static final String INVALID_IMAGE_FILE = "Only image files are allowed";
    public static final String FAILED_TO_UPLOAD_PANTRY_IMAGE = "Failed to upload pantry image";
    public static final String INVALID_BARCODE = "Barcode must contain 8 to 14 digits";
    public static final String PRODUCT_NOT_FOUND_BY_BARCODE = "Product not found for the provided barcode";
    public static final String FAILED_TO_LOOKUP_PRODUCT_BY_BARCODE = "Failed to lookup product by barcode";
    public static final String VALIDATION_FAILED = "Validation failed";
    public static final String INVALID_REQUEST = "Invalid request";
    public static final String UNAUTHORIZED = "Unauthorized";
    public static final String ACCESS_DENIED = "Access denied";
    public static final String INTERNAL_SERVER_ERROR = "Internal server error";
    public static final String AUTHENTICATION_REQUIRED = "Authentication is required";
}
