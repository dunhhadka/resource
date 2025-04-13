package org.example.order.order.infrastructure.data.dto;

import lombok.Getter;
import lombok.Setter;
import org.example.order.order.domain.order.model.Order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;

@Getter
@Setter
public class OrderDto {
    private int storeId;
    private int id;

    private Instant createdOn;
    private Instant modifiedOn;
    private Instant processedOn;
    private Instant closedOn;
    private Instant cancelledOn;

    private Order.CancelReason cancelReason;

    private Order.OrderStatus orderStatus;

    private Order.FinancialStatus financialStatus;

    private Order.FulfillmentStatus fulfillmentStatus;

    private Order.ReturnStatus returnStatus;

    private int totalWeight;

    private boolean test;

    private String note;

    private Integer customerId;
    private String email;
    private String phone;

    private BigDecimal totalPrice;
    private BigDecimal subtotalPrice;
    private BigDecimal totalLineItemPrice;
    private BigDecimal originalTotalPrice;
    private BigDecimal cartDiscountAmount;
    private BigDecimal totalDiscounts;
    private BigDecimal totalShippingPrice;
    private BigDecimal totalTax;
    private BigDecimal currentTotalPrice;
    private BigDecimal currentSubtotalPrice;
    private BigDecimal currentTotalDiscounts;
    private BigDecimal currentCartDiscountAmount;
    private BigDecimal currentTotalTax;
    private BigDecimal totalOutstanding;
    private BigDecimal unpaidAmount;
    private BigDecimal totalRefunded;
    private BigDecimal totalReceived;
    private BigDecimal netPayment;

    private Currency currency;

    private boolean taxExempt;

    private boolean taxIncluded;

    private Integer locationId;
    private Integer userId;
}
