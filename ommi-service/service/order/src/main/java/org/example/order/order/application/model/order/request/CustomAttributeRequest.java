package org.example.order.order.application.model.order.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomAttributeRequest {
    private @Size(max = 50) String name;
    private @Size(max = 50) String value;
}
