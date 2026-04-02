package recipes.householdservice.common.constants;

import lombok.experimental.UtilityClass;

@UtilityClass
public class AppMessages {

    public static final String VALIDATION_FAILED = "Validation failed";
    public static final String UNAUTHORIZED = "Unauthorized";
    public static final String ACCESS_DENIED = "Access denied";
    public static final String INTERNAL_SERVER_ERROR = "Internal server error";
    public static final String AUTHENTICATION_REQUIRED = "Authentication is required";

    public static final String HOUSEHOLD_NOT_FOUND = "Household not found";
    public static final String HOUSEHOLD_MEMBER_NOT_FOUND = "Household member not found";
    public static final String HOUSEHOLD_INVITATION_NOT_FOUND = "Household invitation not found";
    public static final String HOUSEHOLD_SHOPPING_ITEM_NOT_FOUND = "Household shopping item not found";
    public static final String USER_ALREADY_IN_HOUSEHOLD = "User is already a member of this household";
    public static final String HOUSEHOLD_INVITATION_ALREADY_EXISTS = "Pending invitation already exists for this user";
    public static final String HOUSEHOLD_INVITATION_IS_NOT_PENDING = "Household invitation is no longer pending";
    public static final String ONLY_OWNER_CAN_MANAGE_MEMBERS = "Only household owner can manage members";
    public static final String OWNER_CANNOT_BE_REMOVED = "Household owner cannot be removed";
    public static final String CANNOT_INVITE_SELF = "You cannot invite yourself to the household";
    public static final String CANNOT_CREATE_EMPTY_MESSAGE = "Message must not be blank";
    public static final String FAILED_TO_FETCH_PROFILE = "Failed to fetch user profile";
}
