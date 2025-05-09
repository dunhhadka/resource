package org.example.notification.infrastructure.persistent;

import lombok.RequiredArgsConstructor;
import org.example.notification.domain.NotificationSettingRepository;
import org.example.notification.domain.NotificationTemplateSetting;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JpaNotificationTemplateSettingRepositoryImpl implements NotificationSettingRepository {


    @Override
    public NotificationTemplateSetting getTemplateSettingByStoreId(int storeId) {
        return getCurrentTemplateSetting(storeId);
    }

    private NotificationTemplateSetting getCurrentTemplateSetting(int storeId) {
        NotificationTemplateSetting templateSetting;
        // get template setting from StoreSetting (settingValue: JsonString)
        return null;
    }
}
