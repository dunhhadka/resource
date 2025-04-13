package org.example.product.product.application.service.product;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.example.product.product.application.model.images.ImageDownloaded;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public final class ImageDownloadUtils {

    public static ImageDownloaded getImageFromUrl(String url) {
        ImageDownloaded imageDownloaded = null;
        if (StringUtils.isBlank(url)) {
            return null;
        }
        if (url.startsWith("//")) {
            url = "http:" + url;
        }
        if (!url.startsWith("http")) {
            url = "http://" + url;
        }

        if (log.isDebugEnabled()) {
            log.debug("Starting download resouce from url");
        }

        StopWatch watch = new StopWatch();

        try {
            URL path = new URL(url);
            URLConnection connection = path.openConnection();
            connection.setConnectTimeout(1000);
            connection.setReadTimeout(10000);
            InputStream is = null;
            try {
                watch.start();
                is = connection.getInputStream();
                int maxDownload = 2 * 1024 * 1024;
                byte[] buffer = new byte[maxDownload];
                var actualLength = IOUtils.read(is, buffer, 0, maxDownload);
                watch.stop();
                if (is.read() != -1) {
                    log.warn("File too big: " + url);
                } else {
                    String contentType = connection.getContentType();
                    if (ImageDownloaded.isSupportedContentType(contentType)) {
                        imageDownloaded = new ImageDownloaded();
                        imageDownloaded.setContentType(contentType);
                        imageDownloaded.setBytes(Arrays.copyOfRange(buffer, 0, actualLength));
                    }
                }
            } catch (IOException e) {
                if (log.isDebugEnabled()) {
                    log.debug("Error download resource");
                }
            } finally {
                if (watch.isStarted()) watch.stop();

                if (watch.getTime(TimeUnit.MILLISECONDS) > 6000) {
                    log.warn("get data fro url %s slow: %s"
                            .formatted(url, watch.getTime(TimeUnit.MILLISECONDS)));
                }

                if (is != null) {
                    is.close();
                }
            }
        } catch (IOException ignored) {

        }

        return imageDownloaded;
    }

    public static CompletableFuture<ImageDownloaded> getImageFromUrlAsync(String url, ThreadPoolTaskExecutor threadPoolTaskExecutor) {
        return CompletableFuture.supplyAsync(() -> getImageFromUrl(url), threadPoolTaskExecutor);
    }
}
