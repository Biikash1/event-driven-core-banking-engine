package com.banking.api_gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.ServerRequest;

import java.util.function.Function;

@Configuration
public class RateLimiterConfig {

    @Bean
    public Function<ServerRequest, String> keyResolver() {
        // Spring Cloud Gateway MVC uses a standard Java Function for the KeyResolver
        return request -> request.remoteAddress()
                .map(address -> address.getHostString()) // getHostString() safely returns the IP
                .orElse("unknown-ip");
    }
}