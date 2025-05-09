package org.example.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Getter
@Entity
@Table(name = "system_notification_templates")
public class DefaultNotificationTemplate {

    @Id
    private String template;

    private String name;
    private String description;

    @Column(name = "subject")
    private String subjectTemplate;
    @Column(name = "content")
    private String contentTemplate;

    @Enumerated(EnumType.STRING)
    private Type type;

    public enum Type {
        store,
        system
    }
}
