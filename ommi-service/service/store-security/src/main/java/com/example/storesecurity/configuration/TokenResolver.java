package com.example.storesecurity.configuration;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;

import javax.naming.AuthenticationException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;

@Component
public class TokenResolver {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String STORE_ID_HEADER = "Store-Web";
    private static final String AUTHOR_ID_HEADER = "Author-Id_Header";
    private static final String AUTHOR_TYPE_HEADER = "Author-Type_Header";

    public StoreAuthenticationToken resolve(HttpServletRequest request) {
        StoreAuthenticationToken token;
        if (isPrivateIP(request)) {
            token = resolveFromInternalBasicHeader(request);
        } else {
            token = resolveFromPublicBasicHeader(request);
        }
        return null;
    }

    private StoreAuthenticationToken resolveFromPublicBasicHeader(HttpServletRequest request) {
        return null;
    }

    private StoreAuthenticationToken resolveFromInternalBasicHeader(HttpServletRequest request) {
        var header = getHeaderTokenValue(request);
        if (header == null) {
            return null;
        }

        String[] decodeHeaders = decodeHeaderTokenValue(header);

        var storeIdHeader = request.getHeader(STORE_ID_HEADER);
        if (storeIdHeader == null) {
            //throw
        }
        int storeId = Integer.parseInt(storeIdHeader);

        var attributes = new HashMap<String, Object>();

        var authorId = request.getHeader(AUTHOR_ID_HEADER);
        if (authorId != null) {
            attributes.put("AUTHOR_ID", authorId);
        }
        var authorType = request.getHeader(AUTHOR_TYPE_HEADER);
        if (authorType != null) {
            attributes.put("AUTHOR_TYPE", authorType);
        }

        return new StoreAuthenticationToken(authorId, attributes);
    }

    private String[] decodeHeaderTokenValue(String headerToken) {
        var base64Token = headerToken.substring(6).getBytes(StandardCharsets.UTF_8);

        byte[] decoded = Base64.getDecoder().decode(base64Token);

        String token = new String(decoded, StandardCharsets.UTF_8);

        int splitPoint = token.indexOf(":");
        if (splitPoint == -1) {
            throw new BadCredentialsException("Invalid authentication token");
        }

        return new String[]{token.substring(0, splitPoint), token.substring(splitPoint + 1)};
    }

    private String getHeaderTokenValue(HttpServletRequest request) {
        var headerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (headerToken != null && headerToken.toLowerCase().startsWith("basic ")) {
            return headerToken;
        }
        return null;
    }

    private boolean isPrivateIP(HttpServletRequest request) {
        return false;
    }
}
