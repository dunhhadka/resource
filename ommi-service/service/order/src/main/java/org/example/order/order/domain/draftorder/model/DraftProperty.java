package org.example.order.order.domain.draftorder.model;

import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DraftProperty {
    @Size(max = 255)
    private String name;
    @Size(max = 255)
    private String value;
}
