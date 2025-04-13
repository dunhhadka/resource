package org.example.order.order.infrastructure.configuration;

import org.example.AdminClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfig {

    @Bean
    public AdminClient adminClient() {
        return AdminClient.builder().build();
    }
}
