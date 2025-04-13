package org.example.order.order.application.service.draftorder;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class ProductResponse {
    private List<Product> products;

    @Getter
    @Builder
    public static class Product {
        private int id;
        private String name;
        private String alias;
        private String vendor;
        private String productType;
        private String metaTitle;
        private String metaDescription;
        private String summary;
        private String templateLayout;
        private Instant createdOn;
        private Instant modifiedOn;
        private Instant publishedOn;
        private String content;
        private String tags;
        private List<Integer> collections;
        private String type;
    }
}
