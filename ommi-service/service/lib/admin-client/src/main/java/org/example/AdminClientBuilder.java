package org.example;

import feign.Feign;
import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;
import feign.okhttp.OkHttpClient;

public final class AdminClientBuilder {

    private String baseUrl;

    AdminClientBuilder() {
    }

    public AdminClient build() {
        if (this.baseUrl == null || this.baseUrl.isEmpty()) {
            this.baseUrl = "http://localhost:8080";
        }
        return Feign.builder()
                .client(new OkHttpClient())
                .encoder(new GsonEncoder())
                .decoder(new GsonDecoder())
                .target(AdminClient.class, this.baseUrl);
    }
}
