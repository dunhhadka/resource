package org.example.order.order.domain.order.model;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class TracingInfo {
    @Size(max = 50)
    private String source;

    @Size(max = 50)
    private String sourceName;

    @Size(max = 32)
    private String cartToken;

    @Size(max = 32)
    private String checkoutToken;

    @Size(max = 2000)
    private String landingSite;

    @Size(max = 2000)
    private String landingSiteRef;

    @Size(max = 2000)
    private String referringSite;

    // NOTE: "reference" should be the same as "sourceIdentifier", but currently not
    @Size(max = 2000)
    private String reference;

    // ID order from external system
    @Size(max = 255)
    private String sourceIdentifier;

    // URL order from external system
    @Size(max = 255)
    private String sourceUrl;
}
