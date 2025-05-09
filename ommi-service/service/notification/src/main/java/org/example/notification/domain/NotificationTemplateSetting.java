package org.example.notification.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class NotificationTemplateSetting {
    @JsonProperty("color")
    private String color;

    @JsonProperty("logo_width")
    private String logoWidth;

    @JsonProperty("logo_key")
    private String logoKey;
}
