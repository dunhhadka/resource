package com.example.storesecurity.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "security")
public class SecurityProperties {

    private boolean enabled = false;

    private List<AuthenticationMethod> authMethods = List.of();

    public enum AuthenticationMethod {
        public_basic,
        internal_basic,
        bearer,
        cookie
    }
}
