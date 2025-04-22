package com.example.storesecurity.repository;

import com.example.storesecurity.JsonUtils;
import com.example.storesecurity.configuration.PrincipalResolver;
import com.example.storesecurity.configuration.StoreAuthenticatedPrincipal;
import com.example.storesecurity.configuration.StoreAuthenticationToken;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import redis.clients.jedis.JedisPool;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Objects;

public class JdbcCookiePrincipalRepository implements PrincipalResolver {
    private static final String SSO_EMAIL_IDENTITY = "EMAIL";
    private static final String SSO_PHONE_IDENTITY = "PHONE_NUMBER";

    private final JedisPool pool;
    private final JdbcUserRepository userRepository;
    private final JdbcPermissionRepository permissionRepository;

    public JdbcCookiePrincipalRepository(
            JedisPool pool,
            JdbcUserRepository userRepository,
            JdbcPermissionRepository permissionRepository
    ) {
        this.pool = pool;
        this.userRepository = userRepository;
        this.permissionRepository = permissionRepository;
    }

    @Override
    public StoreAuthenticatedPrincipal resolve(StoreAuthenticationToken token) {
        try (var jedis = pool.getResource()) {
            var tokenValue = jedis.get(token.getToken());
            if (tokenValue == null) {
                throw new IllegalArgumentException("Token invalid");
            }

            var tokenInfo = JsonUtils.unmarshal(tokenValue, JsonCookieToken.class);
            int storeId = tokenInfo.getStoreId();

            var usingPhoneIdentity = tokenInfo.sso && Objects.equals(tokenInfo.loginIdentity, SSO_PHONE_IDENTITY);
            var user = usingPhoneIdentity
                    ? this.userRepository.getUserByPhone(storeId, tokenInfo.getUsername())
                    : this.userRepository.getUserByEmail(storeId, tokenInfo.getUsername());
            if (user == null) {
                throw new IllegalArgumentException("User not found");
            }

            var permissions = this.permissionRepository.getPermissions(storeId, user.getId());
            Collection<? extends GrantedAuthority> authorities = new ArrayList<>();
            if (permissions != null) {
                authorities = AuthorityUtils.createAuthorityList(permissions);
            }

            var attributes = new HashMap<String, Object>();
            attributes.put("client_id", user.getId());
            attributes.put("store_id", storeId);

            return new StoreAuthenticatedPrincipal(
                    authorities,
                    attributes,
                    user.getUsername()
            );
        }
    }

    @Override
    public boolean support(StoreAuthenticationToken token) {
        return false;
    }

    @Getter
    @Setter
    static class JsonCookieToken {
        @JsonProperty("store_id")
        private int storeId;
        @JsonProperty("username")
        private String username;
        @JsonProperty("sso")
        private boolean sso;
        @JsonProperty("login_identity")
        private String loginIdentity;
    }
}
