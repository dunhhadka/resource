package org.example.order.order.application.model.orderedit.response;

import lombok.Getter;
import lombok.Setter;
import org.example.order.order.application.model.order.response.OrderResponse;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class CalculatedOrder {
    private int id;
    private int storeId;
    private int orderId;

    private OrderResponse originalOrder;

    private boolean committed;

    private BigDecimal subtotalLineItemQuantity;

    private BigDecimal subtotalPrice;
    private BigDecimal cartDiscountAmount;
    private BigDecimal totalPrice;
    private BigDecimal totalOutstanding;

    private List<CalculatedTaxLine> taxLines;
    private List<CalculatedLineItem> addedLineItems;
    private List<CalculatedDiscountApplication> addedDiscountApplications;

    private List<CalculatedLineItem> lineItems;
    private List<CalculatedLineItem> fulfilledLineItems;
    private List<CalculatedLineItem> unfulfilledLineItems;

    private List<OrderStagedChangeModel> stagedChanges;
}
