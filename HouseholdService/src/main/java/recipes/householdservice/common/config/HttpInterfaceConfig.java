package recipes.householdservice.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import recipes.householdservice.clients.UserProfileClient;

@Configuration
public class HttpInterfaceConfig {

    @Bean
    public UserProfileClient userProfileClient(@Value("${userservice.base-url}") String userServiceBaseUrl,
                                               @Value("${userservice.service-token}") String serviceToken) {
        RestClient restClient = RestClient.builder()
                .baseUrl(userServiceBaseUrl)
                .requestInterceptor((request, body, execution) -> {
                    request.getHeaders().setBearerAuth(serviceToken);
                    return execution.execute(request, body);
                })
                .build();
        RestClientAdapter adapter = RestClientAdapter.create(restClient);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();
        return factory.createClient(UserProfileClient.class);
    }
}
