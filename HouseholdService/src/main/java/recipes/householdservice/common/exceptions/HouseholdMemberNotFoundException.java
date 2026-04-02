package recipes.householdservice.common.exceptions;

public class HouseholdMemberNotFoundException extends RuntimeException {

    public HouseholdMemberNotFoundException(String message) {
        super(message);
    }
}
