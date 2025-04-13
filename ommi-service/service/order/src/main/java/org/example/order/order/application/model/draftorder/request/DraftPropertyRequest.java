package org.example.order.order.application.model.draftorder.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DraftPropertyRequest {
    @Size(max = 250)
    private String name;
    @Size(max = 250)
    private String value;
}
