package org.example.product.product.application.model.product;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.example.product.product.domain.product.model.Product;

import java.time.Instant;
import java.util.List;


@Getter
@Setter
public class ProductRequest {
    private @Size(max = 320) String name;
    private @Size(max = 150) String alias;
    private @Size(max = 255) String vendor;
    private @Size(max = 255) String productType;
    private @Size(max = 320) String metaTitle;
    private @Size(max = 320) String metaDescription;
    private @Size(max = 1000) String summary;
    private Instant publishedOn;
    private @Size(max = 255) String templateLayout;
    private String content;
    private List<@Size(max = 255) String> tags;
    private @Size(max = 255) String defaultVariantUnit;
    private boolean sendWebhooks = true;

    private @Size(max = 250) List<@Valid ProductImageRequest> images;

    private @Size(max = 100) List<@Valid ProductVariantRequest> variants;

    private @Size(max = 3) List<@Valid ProductOptionRequest> options;

    private Product.ProductStatus status;
}
