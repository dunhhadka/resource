package org.example.order.order.infrastructure.data.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;

@Getter
@Setter
public class DraftOrderDto {
    private int id;
    private int storeId;
    private Integer locationId;
    private Integer userId;
    private Integer assigneeId;
    private String sourceName;
    private Instant createdOn;
    private Currency currency;
    private String name;
    private String note;
    private String tags;
    private Instant modifiedOn;
    private Instant completedOn;
    private Integer orderId;
    private String email;
    private String phone;
    private Integer customerId;
    private String lineItems;
    private String shippingAddress;
    private String billingAddress;
    private BigDecimal totalTax;
    private String shippingLine;
    private String noteAttributes;
    private String appliedDiscount;
    private Integer copyOrderId;
    private BigDecimal subtotalPrice;
    private BigDecimal totalPrice;
    private BigDecimal totalDiscounts;
    private BigDecimal totalShippingPrice;
    private BigDecimal totalLineItemsPrice;
    private BigDecimal lineItemsSubtotalPrice;
    private boolean taxesIncluded;
    private Boolean taxExempt;
    private BigDecimal grams;
    private String discountApplications;
}
