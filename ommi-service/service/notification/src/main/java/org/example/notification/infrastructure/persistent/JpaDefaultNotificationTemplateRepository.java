package org.example.notification.infrastructure.persistent;

import org.example.notification.domain.DefaultNotificationTemplate;
import org.example.notification.domain.DefaultNotificationTemplateRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaDefaultNotificationTemplateRepository
        extends JpaRepository<DefaultNotificationTemplate, String>, DefaultNotificationTemplateRepository {

    @Override
    default List<DefaultNotificationTemplate> getAll() {
        return findAllByType(DefaultNotificationTemplate.Type.store);
    }


    @Override
    default DefaultNotificationTemplate getTemplate(String template) {
        return findById(template).orElse(null);
    }

    List<DefaultNotificationTemplate> findAllByType(DefaultNotificationTemplate.Type type);
}
