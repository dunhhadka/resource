package org.example.order.order.application.model.orderedit.response;

import lombok.Getter;
import lombok.Setter;
import org.example.order.order.domain.order.model.LineItem;
import org.example.order.order.infrastructure.data.dao.LineItemDto;
import org.example.order.order.infrastructure.data.dao.OrderEditLineItemDto;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class CalculatedLineItem {
    private String id;

    private BigDecimal quantity;
    private BigDecimal editableQuantity;
    private BigDecimal editableQuantityBeforeChanges;

    private boolean restockable;
    private boolean restocking;

    private BigDecimal originalUnitPrice;
    private BigDecimal discountedUnitPrice;

    private BigDecimal editableSubtotal;
    private BigDecimal uneditableSubtotal;

    private boolean hasStagedLineItemDiscount;

    private List<CalculatedDiscountAllocation> discountAllocations;

    private List<OrderStagedChangeModel> stagedChanges;

    private String sku;
    private String title;
    private String variantTitle;
    private Integer variantId;

    private List<LineItemPropertyResponse> properties;

    public CalculatedLineItem(OrderEditLineItemDto lineItem) {
        this.id = lineItem.getId().toString();

        this.quantity = lineItem.getEditableQuantity();
        this.editableQuantity = lineItem.getEditableQuantity();
        this.editableQuantityBeforeChanges = lineItem.getEditableQuantity();

        this.restockable = lineItem.isRestockable();
        this.restocking = lineItem.isRestockable();

        this.originalUnitPrice = lineItem.getOriginalUnitPrice();
        this.discountedUnitPrice = lineItem.getDiscountedUnitPrice();

        this.editableSubtotal = lineItem.getEditableSubtotal();

        this.hasStagedLineItemDiscount = lineItem.isHasStagedDiscount();

        this.title = lineItem.getTitle();
        this.variantTitle = lineItem.getVariantTitle();
        this.sku = lineItem.getSku();

        this.variantId = lineItem.getVariantId();
    }

    public CalculatedLineItem(LineItemDto lineItem) {
        this.id = String.valueOf(lineItem.getId());

        this.sku = lineItem.getSku();
        this.title = lineItem.getTitle();
        this.variantTitle = lineItem.getVariantTitle();

        this.variantId = lineItem.getVariantId();
        this.restockable = lineItem.isRestockable();

        this.originalUnitPrice = lineItem.getPrice();
        this.discountedUnitPrice = lineItem.getDiscountedUnitPrice();
    }
}
