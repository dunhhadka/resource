package org.example.notification.application.service;

import lombok.RequiredArgsConstructor;
import org.example.notification.application.model.NotificationTemplatePreviewRequest;
import org.example.notification.application.model.NotificationTemplatePreviewResponse;
import org.example.notification.domain.NotificationSettingRepository;
import org.example.notification.domain.NotificationTemplateRepository;
import org.example.notification.domain.NotificationTemplateSetting;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationSender {

    private final NotificationTemplateRepository notificationTemplateRepository;
    private final NotificationSettingRepository notificationSettingRepository;

    public NotificationTemplatePreviewResponse previewTemplate(int storeId, NotificationTemplatePreviewRequest request) {
        var template = request.getTemplate();
        var notificationTemplate = this.notificationTemplateRepository.getByStoreIdAndTemplate(storeId, template);
        if (notificationTemplate == null) {
            throw new IllegalArgumentException("Notification template not found");
        }

        var subject = notificationTemplate.getSubjectTemplate();
        var content = notificationTemplate.getContentTemplate();

        var templateSetting = this.notificationSettingRepository.getTemplateSettingByStoreId(storeId);
        if (request.getSetting() != null) {
            templateSetting = NotificationTemplateSetting.builder()
                    .color(request.getSetting().getColor())
                    .logoWidth(request.getSetting().getLogoWidth())
                    .logoKey(request.getSetting().getLogoKey())
                    .build();
        }

        var globalVariables = createGlobalVariables(storeId, templateSetting);

        var renderResult = renderTemplate(
                storeId,
                subject,
                content,
                globalVariables
        );

        return new NotificationTemplatePreviewResponse();
    }

    private Object renderTemplate(int storeId, String subject, String content, Object globalVariables) {
        String subjectResult, contentResult;

        return null;
    }

    private Object createGlobalVariables(int storeId, NotificationTemplateSetting templateSetting) {
        return null;
    }
}
