package org.example.notification.application.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotificationTemplatePreviewRequest {
    @NotBlank
    private String template;
    private String subject;
    private String content;
    private NotificationTemplateSettingRequest setting;
}
