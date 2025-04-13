package org.example.order.order.application.model.combination.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class CombinationCalculateResponse {

    private List<CombinationLineItemResponse> lineItems;
}
