package org.example.notification.interfaces.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.notification.application.model.NotificationTemplatePreviewRequest;
import org.example.notification.application.model.NotificationTemplatePreviewResponse;
import org.example.notification.application.service.NotificationSender;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/notifications")
public class NotificationController {

    private final NotificationSender notificationSender;

    @PostMapping("/preview_template")
    public NotificationTemplatePreviewResponse templatePreview(@Valid @RequestBody NotificationTemplatePreviewRequest request) {
        int storeId = 1;
        return notificationSender.previewTemplate(storeId, request);
    }
}
