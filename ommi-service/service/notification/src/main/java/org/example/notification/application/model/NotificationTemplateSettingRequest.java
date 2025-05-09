package org.example.notification.application.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotificationTemplateSettingRequest {
    private String color;
    private String logoWidth;
    private String logoKey;
}
