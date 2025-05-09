package org.example.notification.domain;

public interface NotificationTemplateRepository {

    NotificationTemplateDto getByStoreIdAndTemplate(int storeId, String template);

}
