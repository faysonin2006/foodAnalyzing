package com.apigateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Configuration
public class GatewayConfig {

    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> Mono.justOrEmpty(exchange.getRequest().getHeaders().getFirst("Authorization"))
                .flatMap(Mono::just)
                .switchIfEmpty(
                        Mono.fromSupplier(() ->
                                Objects.requireNonNull(exchange.getRequest().getRemoteAddress())
                                        .getAddress().getHostAddress()
                        )
                );
    }


    @Bean
    public RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(10, 20, 10);
    }

    @Bean
    public RouteLocator customRoutLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("userService", p -> p
                        .path("/api/profiles/**")
                        .filters(f -> f
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(redisRateLimiter())
                                        .setKeyResolver(userKeyResolver()))
                                .circuitBreaker(config -> config
                                        .setName("mealBreaker")
                                        .setFallbackUri("forward:/fallback/profiles"))
                                .retry(retryConfig -> retryConfig
                                        .setRetries(3)
                                        .setMethods(HttpMethod.GET)))
                        .uri("lb://USERSERVICE")
                )
                .route("authService", p -> p
                        .path("/api/auth/**")
                        .filters(f -> f
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(redisRateLimiter())
                                        .setKeyResolver(userKeyResolver()))
                                .circuitBreaker(config -> config
                                        .setName("mealBreaker")
                                        .setFallbackUri("forward:/fallback/auth"))
                                .retry(retryConfig -> retryConfig
                                        .setRetries(3)
                                        .setMethods(HttpMethod.GET)))
                        .uri("lb://AUTHSERVICE")
                )
                .route("aiimageService", p -> p
                        .path("/api/food/**")
                        .filters(f -> f
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(redisRateLimiter())
                                        .setKeyResolver(userKeyResolver()))
//                                .circuitBreaker(config -> config
//                                        .setName("aiImageBreaker")
//                                        .setFallbackUri("forward:/fallback/food"))
                                .retry(retryConfig -> retryConfig
                                        .setRetries(3)
                                        .setMethods(HttpMethod.GET)))
                        .uri("lb://AIIMAGESERVICE")
                )
                .build();
    }
}
