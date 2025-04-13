package org.example.product.product.application.model.product;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.example.product.product.domain.product.model.ImagePhysicalInfo;

@Getter
@Setter
@Builder
public class ImageUpdatableInfo {
    private String alt;
    private String src;
    private String fileName;
    private ImagePhysicalInfo physicalInfo;
    private Integer position;
}
