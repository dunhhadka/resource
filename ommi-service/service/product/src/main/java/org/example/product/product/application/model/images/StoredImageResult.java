package org.example.product.product.application.model.images;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StoredImageResult {
    private String src;
    private String fileName;
    private int size;
    private Integer width;
    private Integer height;
}
