package com.recipeservice.configs.httpinterfaceconfig;

import com.recipeservice.clients.RecipesDbClient;
import com.recipeservice.clients.UserPantryClient;
import com.recipeservice.clients.UserProfileClient;
import com.recipeservice.clients.UserShoppingListClient;
import com.recipeservice.configs.spoonacularyclient.SpoonacularClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class HttpInterfaceConfig {

    @Value("${spoonacular.api.key}")
    String apiKey;
    @Value("${userservice.service-token}")
    String serviceToken;
    @Bean
    public SpoonacularClient spoonacularClient(RestClient.Builder builder) {
        RestClient restClient = builder
                .baseUrl("https://api.spoonacular.com")
                .defaultHeader("x-api-key", apiKey)
                .build();
        RestClientAdapter adapter = RestClientAdapter.create(restClient);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();
        return factory.createClient(SpoonacularClient.class);
    }

    @Bean
    public UserProfileClient userProfileClient(@LoadBalanced RestClient.Builder builder) {
        RestClient restClient = builder
                .baseUrl("http://userService")
                .requestInterceptor((request, body, execution) -> {
                    request.getHeaders().setBearerAuth(serviceToken);
                    return execution.execute(request, body);
                })
                .build();
        RestClientAdapter adapter = RestClientAdapter.create(restClient);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();
        return factory.createClient(UserProfileClient.class);
    }

    @Bean
    public UserPantryClient userPantryClient(@LoadBalanced RestClient.Builder builder) {
        RestClient restClient = builder
                .baseUrl("http://userService")
                .requestInterceptor((request, body, execution) -> {
                    request.getHeaders().setBearerAuth(serviceToken);
                    return execution.execute(request, body);
                })
                .build();
        RestClientAdapter adapter = RestClientAdapter.create(restClient);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();
        return factory.createClient(UserPantryClient.class);
    }

    @Bean
    public RecipesDbClient recipesDbClient(@LoadBalanced RestClient.Builder builder) {
        RestClient restClient = builder
                .baseUrl("http://recipesfromdbservice")
                .build();
        RestClientAdapter adapter = RestClientAdapter.create(restClient);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();
        return factory.createClient(RecipesDbClient.class);
    }

    @Bean
    public UserShoppingListClient userShoppingListClient(@LoadBalanced RestClient.Builder builder) {
        RestClient restClient = builder
                .baseUrl("http://userService")
                .requestInterceptor((request, body, execution) -> {
                    request.getHeaders().setBearerAuth(serviceToken);
                    return execution.execute(request, body);
                })
                .build();
        RestClientAdapter adapter = RestClientAdapter.create(restClient);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();
        return factory.createClient(UserShoppingListClient.class);
    }
}
