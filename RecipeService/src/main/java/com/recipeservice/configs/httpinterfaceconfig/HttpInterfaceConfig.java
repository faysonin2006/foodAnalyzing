package com.recipeservice.configs.httpinterfaceconfig;

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
}
