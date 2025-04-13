package org.example.product.product.application.model.images;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;

import java.util.List;

@Slf4j
@Getter
@Setter
public class ImageDownloaded {
    private String contentType;
    private byte[] bytes;

    private static final List<MediaType> supportedContentTypes = List.of(
            MediaType.parseMediaType("image/gif"),
            MediaType.parseMediaType("image/jpeg"),
            MediaType.parseMediaType("image/jpg"),
            MediaType.parseMediaType("image/png"),
            MediaType.parseMediaType("image/webp"),
            MediaType.parseMediaType("image/x-icon"),
            MediaType.parseMediaType("image/svg+xml"));

    public static boolean isSupportedContentType(String contentType) {
        if (StringUtils.isBlank(contentType)) return false;
        try {
            var mediaType = MediaType.parseMediaType(contentType);
            return supportedContentTypes.stream()
                    .anyMatch(mediaType::equals);
        } catch (Exception e) {
            log.warn("Can't parse contentType " + contentType);
        }
        return false;
    }
}
