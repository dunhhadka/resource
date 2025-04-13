package org.example.product.product.application.service.product;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.example.product.product.application.model.images.FileUploadModel;
import org.example.product.product.application.model.images.ImageDownloaded;
import org.example.product.product.application.model.images.StoredImageResult;
import org.example.product.product.application.model.product.ProductImageRequest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageProcessorService {

    private final ThreadPoolTaskExecutor threadPoolTaskExecutor;

    public List<StoredImageResult> storeImage(int storeId, List<ProductImageRequest> images) throws ExecutionException, InterruptedException, IOException {
        return storeImage(storeId, images, ResourceType.product);
    }

    private List<StoredImageResult> storeImage(int storeId, List<ProductImageRequest> imageRequests, ResourceType resourceType) throws ExecutionException, InterruptedException, IOException {
        if (CollectionUtils.isEmpty(imageRequests)) {
            if (log.isDebugEnabled()) {
                log.debug("Skipping store image from request");
            }
            return List.of();
        }

        var fileUploads = prepareFileUploadRequests(imageRequests, storeId, false);
        if (fileUploads.isEmpty()) {
            return List.of();
        }

        return null;
    }

    private List<FileUploadModel> prepareFileUploadRequests(List<ProductImageRequest> imageRequests, int storeId, boolean allowDuplicate) throws ExecutionException, InterruptedException, IOException {
        var fileUploads = new ArrayList<FileUploadModel>();
        var mapByte = downloadAllSrc(imageRequests);

        List<String> existingImage = new ArrayList<>();

        for (var imageReq : imageRequests) {
            if (imageReq.getUploadType() == ProductImageRequest.UploadType.NONE) {
                fileUploads.add(null);
                continue;
            }

            var updateModel = new FileUploadModel();

            var uploadType = imageReq.getUploadType();

            String fileName = detectFileName(imageReq);

            var imageDownloaded = mapByte.get(imageReq.getSrc());

            var contentType = detectContentType(uploadType, fileName, imageReq, imageDownloaded);
            updateModel.setContentType(contentType);

            var binaryContent = detectBinaryContent(uploadType, imageReq, imageDownloaded);
            updateModel.setBytes(binaryContent);

            if (!allowDuplicate) {
                fileName = enrichFileNameWithNotAllowDuplicate(storeId, fileName, existingImage);
            }

            updateModel.setFileName(fileName);

            fileUploads.add(updateModel);
            existingImage.add(fileName);
        }

        return fileUploads;
    }

    private String enrichFileNameWithNotAllowDuplicate(int storeId, String fileName, List<String> existingImage) {
        String extension = FilenameUtils.getExtension(fileName);
        int i = 0;
        while (i < 4) {
            i++;
            String existedImage = null; // GET image from DB BY fileName
            var handleExistedFileName = existingImage.contains(fileName);

            if (existedImage == null && !handleExistedFileName) {
                break;
            }

            if (i == 4) {
                throw new IllegalArgumentException();
            }

            String randomUUID = UUID.randomUUID().toString();
            String baseName = randomUUID.replace("-", "");
            fileName = baseName + "." + extension;
        }

        return fileName;
    }

    private byte[] detectBinaryContent(ProductImageRequest.UploadType uploadType, ProductImageRequest imageRequest, ImageDownloaded imageDownloaded) throws IOException {
        if (uploadType == ProductImageRequest.UploadType.FILE) {
            return imageRequest.getFile().getBytes();
        }
        if (uploadType == ProductImageRequest.UploadType.URL && imageDownloaded != null) {
            return imageDownloaded.getBytes();
        }
        if (uploadType == ProductImageRequest.UploadType.BASE64) {
            return Base64.getDecoder().decode(imageRequest.getBase64());
        }
        return null;
    }

    private String detectContentType(ProductImageRequest.UploadType uploadType, String fileName, ProductImageRequest imageReq, ImageDownloaded imageDownloaded) {
        if (uploadType == ProductImageRequest.UploadType.BASE64) {
            String contentType = FileUtils.getContentType(fileName);

            if (StringUtils.isNotEmpty(contentType)) {
                return contentType;
            }

            return FileUtils.getContentTypeFromBase64(imageReq.getBase64());
        }

        if (uploadType == ProductImageRequest.UploadType.FILE) {
            return imageReq.getFile().getContentType();
        }

        if (uploadType == ProductImageRequest.UploadType.URL && imageDownloaded != null) {
            return imageDownloaded.getContentType();
        }

        return null;
    }

    private String detectFileName(ProductImageRequest imageReq) {
        String fileName = imageReq.getFile() != null
                ? imageReq.getFile().getOriginalFilename()
                : imageReq.getFileName();
        if (StringUtils.isBlank(fileName) && !StringUtils.isBlank(imageReq.getSrc())) {
            try {
                var file = new URL(imageReq.getSrc());
                fileName = file.getFile();
            } catch (MalformedURLException e) {
                log.error("invalid url");
            }
        }

        if (StringUtils.isBlank(fileName)) {
            fileName = UUID.randomUUID().toString();
        }

        fileName = fileName.toLowerCase();

        return fileName;
    }

    private LinkedHashMap<String, ImageDownloaded> downloadAllSrc(List<ProductImageRequest> imageRequests) throws ExecutionException, InterruptedException {

        var mapFuture = new LinkedHashMap<String, CompletableFuture<ImageDownloaded>>();

        var downloadSrcs = imageRequests.stream()
                .filter(Objects::nonNull)
                .map(ProductImageRequest::getSrc)
                .filter(StringUtils::isNotBlank)
                .distinct().toList();

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        for (var src : downloadSrcs) {
            var urlBuilder = UriComponentsBuilder.fromUriString(URLDecoder.decode(src, StandardCharsets.UTF_8));
            var encodeSrc = urlBuilder.build(false).encode().toUriString();
            mapFuture.put(src, ImageDownloadUtils.getImageFromUrlAsync(encodeSrc, threadPoolTaskExecutor));
        }

        allOfExecuteTask(mapFuture.values());

        stopWatch.stop();

        log.info("Get resource: " + stopWatch.getTime(TimeUnit.MILLISECONDS));

        var mapByte = new LinkedHashMap<String, ImageDownloaded>();
        for (var entry : mapFuture.entrySet()) {
            ImageDownloaded downloaded = null;
            if (entry.getValue().isDone() && !entry.getValue().isCompletedExceptionally()) {
                downloaded = entry.getValue().get();
            }
            mapByte.put(entry.getKey(), downloaded);
        }

        return mapByte;
    }

    private void allOfExecuteTask(Collection<CompletableFuture<ImageDownloaded>> futures) {
        CompletableFuture<Void> allFutureResult = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        allFutureResult.thenApply(v -> futures.stream().map(CompletableFuture::join).toList());
    }

    enum ResourceType {
        product
    }
}
