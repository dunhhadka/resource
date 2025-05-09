package org.example.notification.domain;

public interface NotificationSettingRepository {
    NotificationTemplateSetting getTemplateSettingByStoreId(int storeId);
}
