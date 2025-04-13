package org.example.order.order.domain.transaction.model;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class ClientInfo {
    private Integer userId;
    @Size(max = 64)
    private String clientId;
    @Size(max = 50)
    private String deviceId;
}
