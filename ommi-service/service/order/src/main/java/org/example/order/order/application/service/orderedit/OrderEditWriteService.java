package org.example.order.order.application.service.orderedit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.example.location.Location;
import org.example.order.order.application.model.orderedit.request.OrderEditRequest;
import org.example.order.order.application.service.order.OrderWriteService;
import org.example.order.order.domain.edit.model.AddedLineItem;
import org.example.order.order.domain.edit.model.OrderEdit;
import org.example.order.order.domain.edit.model.OrderEditId;
import org.example.order.order.domain.edit.persistence.OrderEditIdGenerator;
import org.example.order.order.domain.edit.persistence.OrderEditRepository;
import org.example.order.order.domain.order.model.*;
import org.example.order.order.domain.order.persistence.OrderRepository;
import org.example.order.order.domain.refund.model.OrderAdjustment;
import org.example.order.order.infrastructure.configuration.exception.ConstrainViolationException;
import org.example.order.order.infrastructure.data.dto.ProductDto;
import org.example.order.order.infrastructure.data.dto.VariantDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEditWriteService {

    private final OrderEditIdGenerator orderEditIdGenerator;

    private final OrderRepository orderRepository;
    private final OrderEditRepository orderEditRepository;

    private final OrderEditContext orderEditContext;

    private final OrderWriteService orderWriteService;

    @Transactional
    public OrderEditId begin(int storeId, int orderId) {
        var order = this.findOrderById(storeId, orderId);
        var currency = order.getMoneyInfo().getCurrency();

        BigDecimal subtotalLineItemQuantity = BigDecimal.ZERO;
        BigDecimal subtotalPrice = BigDecimal.ZERO;

        BigDecimal cartDiscountAmount = BigDecimal.ZERO;

        BigDecimal shippingPrice = BigDecimal.ZERO;
        BigDecimal shippingRefundAmount = BigDecimal.ZERO;

        BigDecimal taxPrice = BigDecimal.ZERO;

        var refundedQuantityMap = new HashMap<Integer, BigDecimal>();
        if (CollectionUtils.isNotEmpty(order.getRefunds())) {
            for (var refund : order.getRefunds()) {

                if (CollectionUtils.isNotEmpty(refund.getRefundLineItems())) {
                    refund.getRefundLineItems().forEach(refundLineItem -> {
                        refundedQuantityMap.merge(
                                refundLineItem.getLineItemId(),
                                BigDecimal.valueOf(refundLineItem.getQuantity()),
                                BigDecimal::add
                        );
                    });
                }

                if (CollectionUtils.isNotEmpty(refund.getOrderAdjustments())) {
                    for (var adjustment : refund.getOrderAdjustments()) {
                        if (adjustment.getKind() == OrderAdjustment.RefundKind.shipping_refund) {
                            var refundAmount = adjustment.getAmount().add(adjustment.getTaxAmount());
                            shippingRefundAmount = shippingRefundAmount.add(refundAmount);
                        }
                    }
                }
            }
        }

        for (var shippingLine : order.getShippingLines()) {
            shippingPrice = shippingPrice.add(shippingLine.getPrice());

            for (var discount : shippingLine.getDiscountAllocations()) {
                cartDiscountAmount = cartDiscountAmount.add(discount.getAmount());
            }
        }

        for (var lineItem : order.getLineItems()) {
            var refundedQuantity = refundedQuantityMap.getOrDefault(lineItem.getId(), BigDecimal.ZERO);
            var quantityDecimal = BigDecimal.valueOf(lineItem.getQuantity());
            var effectiveQuantity = quantityDecimal.subtract(refundedQuantity);
            if (effectiveQuantity.signum() <= 0) {
                continue;
            }

            subtotalLineItemQuantity = subtotalLineItemQuantity.add(effectiveQuantity);

            var originalQuantity = BigDecimal.valueOf(lineItem.getQuantity());

            var effectivePrice = lineItem.getPrice().multiply(effectiveQuantity);
            subtotalPrice = subtotalPrice.add(effectivePrice);

            BigDecimal productDiscount = BigDecimal.ZERO;
            BigDecimal cartDiscount = BigDecimal.ZERO;
            for (var discount : lineItem.getDiscountAllocations()) {
                if (isProductDiscount(discount, order.getDiscountApplications())) {
                    productDiscount = productDiscount.add(discount.getAmount());
                } else {
                    cartDiscount = cartDiscount.add(discount.getAmount());
                }
            }

            var effectiveProductDiscount = productDiscount.multiply(effectiveQuantity)
                    .divide(originalQuantity, currency.getDefaultFractionDigits(), RoundingMode.FLOOR);
            subtotalPrice = subtotalPrice.subtract(effectiveProductDiscount);

            var effectiveCartDiscount = cartDiscount.multiply(effectiveQuantity)
                    .divide(originalQuantity, currency.getDefaultFractionDigits(), RoundingMode.FLOOR);
            cartDiscountAmount = cartDiscountAmount.add(effectiveCartDiscount);

            if (!order.isTaxIncluded() && CollectionUtils.isNotEmpty(lineItem.getTaxLines())) {
                var lineTaxPrice = lineItem.getTaxLines().stream()
                        .map(TaxLine::getPrice)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                var effectiveTax = lineTaxPrice.multiply(effectiveQuantity)
                        .divide(originalQuantity, currency.getDefaultFractionDigits(), RoundingMode.FLOOR);
                taxPrice = taxPrice.add(effectiveTax);
            }
        }

        var totalPrice = subtotalPrice.subtract(cartDiscountAmount)
                .add(shippingPrice)
                .subtract(shippingRefundAmount)
                .add(taxPrice);

        var totalOutstanding = order.getMoneyInfo().getTotalOutstanding()
                .subtract(order.getMoneyInfo().getCurrentTotalPrice())
                .add(totalPrice);

        var orderEditId = new OrderEditId(storeId, this.orderEditIdGenerator.generateOrderEditId());

        var orderEdit = new OrderEdit(
                orderEditId,
                orderId,
                currency,
                subtotalLineItemQuantity,
                subtotalPrice,
                cartDiscountAmount,
                totalPrice,
                totalOutstanding
        );

        this.orderEditRepository.save(orderEdit);

        return orderEditId;
    }

    private boolean isProductDiscount(DiscountAllocation discount, List<DiscountApplication> discountApplications) {
        var discountApplicationIndex = discount.getApplicationIndex();
        var discountApplication = discountApplications.get(discountApplicationIndex);
        return discountApplication.getRuleType() == DiscountApplication.RuleType.product;
    }

    private Order findOrderById(int storeId, int orderId) {
        var fetchedOrder = this.orderRepository.findById(new OrderId(storeId, orderId));
        if (fetchedOrder == null) {
            throw new ConstrainViolationException("order", "order not found");
        }
        if (fetchedOrder.getClosedOn() != null) {
            throw new ConstrainViolationException(
                    "base",
                    "closed order can't be edited"
            );
        }
        if (fetchedOrder.getCancelledOn() != null) {
            throw new ConstrainViolationException(
                    "base",
                    "cancelled order can't be edited"
            );
        }
        return fetchedOrder;
    }

    @Transactional
    public List<UUID> addVariants(OrderEditId orderEditId, List<OrderEditRequest.AddVariant> requests) {
        var context = this.orderEditContext.createContext(orderEditId, requests);

        List<UUID> lineItemIds = requests.stream()
                .map(request -> addVariant(request, context))
                .toList();

        this.orderEditRepository.save(context.orderEdit());

        return lineItemIds;
    }

    private UUID addVariant(OrderEditRequest.AddVariant request, OrderEditContext.AddVariantsContext context) {
        var editing = context.orderEdit();

        var variant = context.getVariantById(request.getVariantId());
        var product = context.getProductById(variant.getProductId());
        var location = context.getLocationById(request.getLocationId());

        var newLineItem = buildNewVariantLineItem(variant, product, location, request);

        editing.addLineItem(newLineItem, context.taxContext());

        return newLineItem.getId();
    }

    private AddedLineItem buildNewVariantLineItem(VariantDto variant, ProductDto product, Location location, OrderEditRequest.AddVariant request) {
        return new AddedLineItem(
                UUID.randomUUID(),
                variant.getId(),
                product.getId(),
                (int) location.getId(),
                product.getName(),
                variant.getTitle(),
                variant.isTaxable(),
                variant.isRequiresShipping(),
                true,
                request.getQuantity(),
                variant.getPrice()
        );
    }

    @Transactional
    public UUID addCustomItem(OrderEditId orderEditId, OrderEditRequest.AddCustomItem request) {
        var context = this.orderEditContext.createContext(orderEditId, request);

        var editing = context.orderEdit();

        var newLineItem = buildNewCustomItem(context.request(), context.getLocation());
        editing.addLineItem(newLineItem, context.taxContext());

        this.orderEditRepository.save(editing);

        return newLineItem.getId();
    }

    private AddedLineItem buildNewCustomItem(OrderEditRequest.AddCustomItem request, Location location) {
        return new AddedLineItem(
                UUID.randomUUID(),
                null,
                null,
                (int) location.getId(),
                request.getTitle(),
                null,
                request.isTaxable(),
                request.isRequireShipping(),
                false,
                request.getQuantity(),
                request.getPrice()
        );
    }

    @Transactional
    public String updateItemQuantity(OrderEditId orderEditId, OrderEditRequest.SetItemQuantity request) {
        var context = this.orderEditContext.createContext(orderEditId, request);

        var editing = context.orderEdit();
        boolean isUpdated = false;

        if (context instanceof OrderEditContext.RemovedAddedItemContext removedItem) {
            UUID addedLineItemId = removedItem.lineItem().getId();

            isUpdated = editing.removeAddedLineItem(addedLineItemId, removedItem.taxContext());

        } else if (context instanceof OrderEditContext.AdjustAddedItemContext adjustAddedItemContext) {
            UUID lineItemId = adjustAddedItemContext.lineItem().getId();

            isUpdated = editing.adjustAddedLineItem(lineItemId, adjustAddedItemContext.taxContext(), context.request().getQuantity());
        } else if (context instanceof OrderEditContext.SetQuantityExistingLineContext setQuantityContext) {
            LineItem lineItem = setQuantityContext.lineItem();

            boolean isFulfilled = lineItem.getFulfillableQuantity() == 0;
            if (isFulfilled) {
                throw new ConstrainViolationException("line_item", "Fulfilled Item can't be edited");
            }

            boolean hasStagedDiscount = CollectionUtils.isNotEmpty(lineItem.getDiscountAllocations());
            if (hasStagedDiscount) {
                throw new ConstrainViolationException("line_item", "Quantity cannot adjusted due to the discount applied");
            }

            isUpdated = editing.updateExistingLineQuantity(
                    lineItem,
                    request.getQuantity(),
                    request.isRestock(),
                    setQuantityContext.taxContext());
        }

        if (isUpdated) {
            this.orderEditRepository.save(editing);
        }

        return request.getLineItemId();
    }

    @Transactional
    public UUID setItemDiscount(OrderEditId orderEditId, OrderEditRequest.SetItemDiscount request) {
        var context = this.orderEditContext.createContext(orderEditId, request);

        var editing = context.orderEdit();
        var discountRequest = context.discountRequest();

        if (log.isDebugEnabled()) {
            log.debug("Receiving apply discount request for line item id {} tha result in {}",
                    context.lineItem().getId(),
                    discountRequest.value().signum() == 0 ? "a remove" : "an edit");
        }

        UUID lineItemId = context.lineItem().getId();
        if (discountRequest.value().signum() == 0) { // Remove Item discount
            editing.removeLineItemDiscount(lineItemId, context.taxContext());
            return lineItemId;
        }

        editing.applyDiscount(lineItemId, discountRequest, context.taxContext());

        this.orderEditRepository.save(editing);

        return lineItemId;
    }

    @Transactional
    public OrderId commit(OrderEditId orderEditId, OrderEditRequest.Commit request) {
        var orderEdit = this.findOrderEditById(orderEditId);

        var orderId = new OrderId(orderEditId.getStoreId(), orderEdit.getOrderId());

        this.orderWriteService.editLineItems(orderId, orderEdit, request);

        orderEditRepository.save(orderEdit);
        return orderId;
    }

    private OrderEdit findOrderEditById(OrderEditId orderEditId) {
        var orderEdit = this.orderEditRepository.findById(orderEditId);
        if (orderEdit == null) {
            throw new ConstrainViolationException(
                    "order_edit",
                    "Order edit not found by id = " + orderEdit
            );
        }
        if (orderEdit.isCommitted()) {
            throw new ConstrainViolationException(
                    "order_edit",
                    "commit"
            );
        }
        return orderEdit;
    }
}
