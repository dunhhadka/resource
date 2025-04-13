package org.example.product.product.application.model.product;

import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@Setter
@Builder
public class ProductImageRequest {

    public enum UploadType {NONE, BASE64, URL, FILE}

    private Integer id;
    private String base64;
    private String src;
    private MultipartFile file;
    private String fileName;
    private @Size(max = 255) String alt;
    private List<Integer> variantIds;
    private Integer position;

    @Builder.Default
    private boolean sendWebhooks = true;

    public UploadType getUploadType() {
        if (this.file != null) return UploadType.FILE;
        if (StringUtils.isNotBlank(this.base64)) return UploadType.BASE64;
        if (StringUtils.isNotBlank(this.src)) return UploadType.URL;
        return UploadType.NONE;
    }
}
