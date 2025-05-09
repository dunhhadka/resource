package org.example.notification.infrastructure.persistent;

import org.example.notification.domain.NotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreNotificationTemplateRepository extends JpaRepository<NotificationTemplate, Integer> {

    NotificationTemplate findByStoreIdAndTemplate(int storeId, String template);

}
