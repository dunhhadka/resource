package org.example.notification.infrastructure.persistent;

import lombok.RequiredArgsConstructor;
import org.example.notification.domain.DefaultNotificationTemplate;
import org.example.notification.domain.DefaultNotificationTemplateRepository;
import org.example.notification.domain.NotificationTemplate;
import org.example.notification.domain.NotificationTemplateDto;
import org.example.notification.domain.NotificationTemplateRepository;
import org.springframework.stereotype.Repository;
import org.springframework.util.Assert;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaNotificationTemplateRepository implements NotificationTemplateRepository {

    private final DefaultNotificationTemplateRepository defaultNotificationTemplateRepository;
    private final StoreNotificationTemplateRepository storeNotificationTemplateRepository;

    @Override
    public NotificationTemplateDto getByStoreIdAndTemplate(int storeId, String template) {
        var defaultTemplate = this.defaultNotificationTemplateRepository.getTemplate(template);
        var storeTemplate = defaultTemplate != null ? storeNotificationTemplateRepository.findByStoreIdAndTemplate(storeId, template) : null;
        if (defaultTemplate == null || defaultTemplate.getType() != DefaultNotificationTemplate.Type.store) {
            return null;
        }
        return toStoreNotificationTemplate(storeId, storeTemplate, defaultTemplate);
    }

    private NotificationTemplateDto toStoreNotificationTemplate(
            int storeId,
            NotificationTemplate storeTemplate,
            DefaultNotificationTemplate defaultTemplate
    ) {
        Assert.notNull(defaultTemplate, "defaultTemplate must not be null");

        String subject = Optional.ofNullable(storeTemplate)
                .map(NotificationTemplate::getSubjectTemplate)
                .orElse(defaultTemplate.getSubjectTemplate());
        String content = Optional.ofNullable(storeTemplate)
                .map(NotificationTemplate::getContentTemplate)
                .orElse(defaultTemplate.getContentTemplate());

        var template = defaultTemplate.getTemplate();

        return NotificationTemplateDto.builder()
                .storeId(storeId)
                .template(template)
                .name(defaultTemplate.getName())
                .description(defaultTemplate.getDescription())
                .subjectTemplate(subject)
                .contentTemplate(content)
                .build();
    }
}
