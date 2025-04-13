package org.example.order.order.application.model.combination.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class CombinationCalculateRequest {

    //true - Tính toán trả ra thông tin mới nhất của sản phảm, false - chỉ chia lại discount_allocations và phân bổ lại thuế nếu calculateTax = true
    private boolean updateProductInfo;

    private boolean calculateTax;

    private boolean taxExempt;

    private boolean taxIncluded;

    @Builder.Default
    private String currency = "VND";

    @Builder.Default
    private String countryCode = "VN";

    private @NotEmpty List<@Valid CombinationLineItemRequest> lineItems;
}
