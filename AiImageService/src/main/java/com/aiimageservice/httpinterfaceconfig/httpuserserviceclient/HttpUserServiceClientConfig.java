package com.aiimageservice.httpinterfaceconfig.httpuserserviceclient;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class HttpUserServiceClientConfig {

    @Bean
    public HttpUserServiceClient httpUserServiceClient(@LoadBalanced RestClient.Builder builder,
                                                       @Value("${userservice.service-token}") String serviceToken) {
        RestClient restClient = builder
                .baseUrl("http://userService")
                .requestInterceptor(((request, body, execution) -> {
                    request.getHeaders().setBearerAuth(serviceToken);
                    return execution.execute(request, body);
                }))
                .build();
        RestClientAdapter restClientAdapter = RestClientAdapter.create(restClient);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(restClientAdapter).build();
        return factory.createClient(HttpUserServiceClient.class);
    }
}
