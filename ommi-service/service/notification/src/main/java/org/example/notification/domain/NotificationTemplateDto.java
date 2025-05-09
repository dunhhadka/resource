package org.example.notification.domain;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationTemplateDto {
    private int storeId;

    private String template;

    private String name;
    private String description;

    private String subjectTemplate;
    private String contentTemplate;
}
