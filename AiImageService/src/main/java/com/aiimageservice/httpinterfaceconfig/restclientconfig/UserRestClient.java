package com.aiimageservice.httpinterfaceconfig.restclientconfig;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestClient;

@Configuration
public class UserRestClient {

    @Bean
    @Primary
    public RestClient.Builder primaryRestClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    @LoadBalanced
    public RestClient.Builder balancedRestClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    @LoadBalanced
    public RestClient restClient(@LoadBalanced RestClient.Builder builder) {
        return builder.build();
    }
}
