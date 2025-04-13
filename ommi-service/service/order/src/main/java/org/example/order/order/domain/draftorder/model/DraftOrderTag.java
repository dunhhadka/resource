package org.example.order.order.domain.draftorder.model;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
@Embeddable
public class DraftOrderTag {
    @Size(max = 250)
    private String name;

    @Size(max = 250)
    private String alias;
}
