package recipes.householdservice.clients;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import recipes.householdservice.dto.internal.profile.UserProfileResponse;

@HttpExchange("/api/profiles/internal")
public interface UserProfileClient {

    @GetExchange("/{email}")
    UserProfileResponse getProfile(@PathVariable String email);
}
