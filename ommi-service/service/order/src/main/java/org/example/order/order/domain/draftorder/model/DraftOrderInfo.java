package org.example.order.order.domain.draftorder.model;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Currency;

@Builder(toBuilder = true)
@Getter
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class DraftOrderInfo {
    @NotNull
    private Currency currency;

    @Size(max = 2000)
    private String note;

    @Size(max = 128)
    private String email;

    @Size(max = 250)
    private String phone;

    private String sourceName;

    private Boolean taxExempt;

    private Integer locationId;

    private Integer assigneeId;
}
