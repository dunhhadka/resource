package org.example.order.order.application.model.orderedit.response;

import lombok.Getter;
import lombok.Setter;
import org.example.order.order.infrastructure.data.dao.DiscountAllocationDto;
import org.example.order.order.infrastructure.data.dao.OrderEditDiscountAllocationDto;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class CalculatedDiscountAllocation {
    private BigDecimal allocatedAmount;
    private String discountApplicationId;

    public CalculatedDiscountAllocation(OrderEditDiscountAllocationDto allocation) {
        this.allocatedAmount = allocation.getAmount();
        this.discountApplicationId = allocation.getApplicationId().toString();
    }

    public CalculatedDiscountAllocation(DiscountAllocationDto allocation) {
        this.allocatedAmount = allocation.getAmount();
        this.discountApplicationId = String.valueOf(allocation.getApplicationId());
    }
}
