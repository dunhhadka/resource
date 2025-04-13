package org.example.order.order.application.model.draftorder.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class DraftShippingLineRequest {
    private boolean custom;
    private String alias;

    @NotBlank
    @Size(max = 150)
    private String title;

    @NotNull
    @Min(0)
    private BigDecimal price;

    @Size(max = 50)
    private String source;

    private String code;
}
