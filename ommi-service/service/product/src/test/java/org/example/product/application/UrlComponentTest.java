package org.example.product.application;


import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
public class UrlComponentTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "https%3A%2F%2Fexample.com%2Fsearch%3Fq%3DJava%26page%3D1"
    })
    void standardize(String url) {
        // Giải mã URL
        String decodedUrl = URLDecoder.decode(url, StandardCharsets.UTF_8);

        // Tạo URI và chuẩn hóa nó
        var uriBuilder = UriComponentsBuilder.fromUriString(decodedUrl);
        String validUrl = uriBuilder.encode().toUriString(); // Mã hóa lại URL

        String encodeUrl = URLEncoder.encode(validUrl, StandardCharsets.UTF_8);

        // Kiểm tra kết quả
        assertEquals("https://example.com/search?q=Java&page=1", decodedUrl);
        assertEquals("https%3A%2F%2Fexample.com%2Fsearch%3Fq%3DJava%26page%3D1", encodeUrl);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://example.com/path/to/resource?name=John&age=25"
    })
    void analystUrl(String url) {
        var uriComponents = UriComponentsBuilder.fromUriString(URLDecoder.decode(url, StandardCharsets.UTF_8)).build();

        assertEquals("https", uriComponents.getScheme());
        assertEquals("example.com", uriComponents.getHost());
        assertEquals("/path/to/resource", uriComponents.getPath());
    }
}
