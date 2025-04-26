package com.example.storesecurity.configuration;

import com.example.storesecurity.cache.LRUCache;
import com.example.storesecurity.repository.JdbcCookiePrincipalRepository;
import com.example.storesecurity.repository.JdbcPermissionRepository;
import com.example.storesecurity.repository.JdbcUserRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import javax.sql.DataSource;
import java.util.List;

@Configuration
@EnableConfigurationProperties(SecurityProperties.class)
@ConditionalOnProperty(name = "security.enabled", matchIfMissing = true, havingValue = "true")
public class SecurityConfiguration {

    @Bean
    public String startSecurity() {
        System.out.println("Start configuration security");
        return "Security configuration success";
    }

    @Configuration
    @ConfigurationOnAuthenticationMethod(SecurityProperties.AuthenticationMethod.cookie)
    static class CookieConfiguration {

        @Bean
        public String startCookieConfiguration() {
            System.out.println("Start cookie configuration");
            return "Cookie configuration success";
        }

        @Bean
        @ConditionalOnMissingBean(name = "cookieRedisProperties")
        @ConfigurationProperties(prefix = "spring.redis-common")
        public RedisProperties cookieRedisProperties() {
            return new RedisProperties();
        }

        @Bean
        @ConditionalOnMissingBean
        public JdbcCookiePrincipalRepository cookiePrincipalRepository(
                @Qualifier("cookieRedisProperties") RedisProperties cookieRedisProperties,
                JdbcUserRepository userRepository,
                JdbcPermissionRepository permissionRepository,
                RedisProperties redisProperties) {
            return new JdbcCookiePrincipalRepository(
                    redisProperties.getPool(),
                    userRepository,
                    permissionRepository
            );
        }
    }

    @Configuration
    @EnableWebSecurity
    static class CommonConfiguration {

        @Bean(DataSourceNames.STORE_SECURITY)
        @ConditionalOnMissingBean(name = DataSourceNames.STORE_SECURITY)
        @ConfigurationProperties(prefix = "spring.store-datasource")
        public DataSource primaryDataSource() {
            return DataSourceBuilder.create().build();
        }

        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnBean(name = DataSourceNames.STORE_SECURITY)
        public JdbcUserRepository defaultUserRepository(@Qualifier(DataSourceNames.STORE_SECURITY) DataSource dataSource) {
            return new JdbcUserRepository(new NamedParameterJdbcTemplate(dataSource), new LRUCache<>(20));
        }

        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnBean(name = DataSourceNames.STORE_SECURITY)
        public JdbcPermissionRepository defaultPermissionRepository(@Qualifier(DataSourceNames.STORE_SECURITY) DataSource dataSource) {
            return new JdbcPermissionRepository(new JdbcTemplate(dataSource), new LRUCache<>(20));
        }

        @Bean
        @ConditionalOnMissingBean
        @Order(org.springframework.boot.autoconfigure.security.SecurityProperties.BASIC_AUTH_ORDER)
        SecurityFilterChain securityFilterChain(
                HttpSecurity http,
                List<SecurityCustomizer> securityCustomizers,
                StoreSecurityConfigurer securityConfigurer
        ) throws Exception {
            http.authorizeHttpRequests(authz -> {
                authz.requestMatchers("/actuator/**").permitAll();
                if (securityCustomizers.isEmpty()) {
                    authz.anyRequest().authenticated();
                } else {
                    for (var customizer : securityCustomizers) {
                        customizer.configure(authz);
                    }
                }
            });

            http.with(securityConfigurer, configurer -> {
            });

            http.requestCache(AbstractHttpConfigurer::disable);
            http.logout(AbstractHttpConfigurer::disable);
            http.csrf(AbstractHttpConfigurer::disable);
            http.sessionManagement(config -> config.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
            return http.build();
        }

        @Bean
        @ConditionalOnMissingBean
        public StoreAuthenticationProvider authenticationProvider(List<PrincipalResolver> principalResolvers) {
            return new StoreAuthenticationProvider(principalResolvers);
        }

        @Bean
        @ConditionalOnMissingBean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration, StoreAuthenticationProvider provider) throws Exception {
            ProviderManager providerManager = (ProviderManager) configuration.getAuthenticationManager();
            providerManager.getProviders().add(provider);
            return providerManager;
        }
    }
}
