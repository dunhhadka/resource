package org.example.product.product.application.model.images;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FileUploadModel {
    private String contentType;
    private String fileName;
    private byte[] bytes;
}
