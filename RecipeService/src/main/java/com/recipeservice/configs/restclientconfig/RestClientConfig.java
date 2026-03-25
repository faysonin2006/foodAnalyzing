package com.recipeservice.configs.restclientconfig;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Primary
    @Bean
    public RestClient.Builder defaultBuilder() {
        return RestClient.builder();
    }

    @LoadBalanced
    @Bean
    public RestClient.Builder balancedBuilder() {
        return RestClient.builder();
    }

    @Bean
    public RestClient restClient(@LoadBalanced RestClient.Builder builder) {
        return builder.build();
    }
}
