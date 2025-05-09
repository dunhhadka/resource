package org.example.notification.domain;

import java.util.List;

public interface DefaultNotificationTemplateRepository {
    List<DefaultNotificationTemplate> getAll();

    DefaultNotificationTemplate getTemplate(String template);
}
