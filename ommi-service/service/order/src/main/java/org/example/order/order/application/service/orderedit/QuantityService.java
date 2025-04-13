package org.example.order.order.application.service.orderedit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.example.order.order.application.model.order.request.OrderTransactionCreateRequest;
import org.example.order.order.application.model.refund.request.RefundRequest;
import org.example.order.order.application.model.refund.response.RefundCalculationResponse;
import org.example.order.order.application.service.order.RefundCalculationService;
import org.example.order.order.application.utils.NumberUtils;
import org.example.order.order.application.utils.OrderEditUtils;
import org.example.order.order.domain.edit.model.OrderStagedChange;
import org.example.order.order.domain.fulfillmentorder.model.FulfillmentOrder;
import org.example.order.order.domain.fulfillmentorder.model.FulfillmentOrderLineItem;
import org.example.order.order.domain.fulfillmentorder.persistence.FulfillmentOrderRepository;
import org.example.order.order.domain.order.model.LineItem;
import org.example.order.order.domain.order.model.Order;
import org.example.order.order.domain.order.model.OrderId;
import org.example.order.order.domain.order.model.ShippingLine;
import org.example.order.order.domain.order.model.TaxLine;
import org.example.order.order.domain.order.persistence.OrderIdGenerator;
import org.example.order.order.domain.refund.model.OrderAdjustment;
import org.example.order.order.domain.refund.model.Refund;
import org.example.order.order.domain.refund.model.RefundLineItem;
import org.example.order.order.domain.refund.model.RefundTaxLine;
import org.example.order.order.domain.transaction.model.OrderTransaction;
import org.example.order.order.domain.transaction.persistence.TransactionRepository;
import org.example.order.order.infrastructure.configuration.exception.ConstrainViolationException;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuantityService {

    private final OrderIdGenerator orderIdGenerator;

    private final FulfillmentOrderRepository fulfillmentOrderRepository;
    private final TransactionRepository transactionRepository;

    private final RefundCalculationService refundCalculationService;

    public List<Pair<LineItem, BigDecimal>> increaseItems(
            Order order,
            OrderEditUtils.GroupedStagedChange changes
    ) {
        return order.increaseLineItems(
                changes.incrementItems().stream()
                        .collect(Collectors.toMap(
                                OrderStagedChange.IncrementItem::getLineItemId,
                                OrderStagedChange.IncrementItem::getDelta
                        ))
        );
    }

    public Refund decreaseItems(
            Order order,
            OrderEditUtils.GroupedStagedChange changes,
            Warnings.Builder warningsBuilder
    ) {
        if (changes.decrementItems().isEmpty()) return null;

        // Map [Key: LineItemId] -> [Value: remainingQuantity of Line]
        HashMap<Integer, Integer> remainingQuantityMap = new HashMap<>();

        var fulfillmentLineItemMap = fetchFulfillmentOrders(order.getId());

        List<RefundRequest.LineItem> refundItemRequests = new ArrayList<>();

        for (var decrement : changes.decrementItems()) {
            LineItem lineItem = getQuantityReducibleLineItem(decrement, order, remainingQuantityMap, warningsBuilder);

            if (lineItem == null) {
                continue;
            }

            boolean shouldRestock = false;
            if (decrement.isRestock()) {
                if (lineItem.getVariantInfo().isProductExists()) shouldRestock = true;
                else {
                    warningsBuilder.add(
                            "refund",
                            "DecrementItem %s reference a non-restockable LineItem %s"
                                    .formatted(changes.find(decrement), lineItem.getId())
                    );
                }
            }

            if (log.isDebugEnabled()) {
                log.debug("DecrementItem {} results in a {} refund",
                        decrement, shouldRestock ? "restock" : "non-restock");
            }

            createRefundRequests(refundItemRequests, decrement, shouldRestock, fulfillmentLineItemMap);
        }

        if (refundItemRequests.isEmpty()) return null;

        var refundOption = RefundRequest.Option.DEFAULT;
        var refundRequest = new RefundRequest();
        refundRequest.setOption(refundOption);
        refundRequest.setRefundLineItems(refundItemRequests);

        var refundResult = addRefund(order, refundRequest);

        return refundResult.refund;
    }

    private RefundResult addRefund(Order order, RefundRequest refundRequest) {
        var moneyInfo = order.getMoneyInfo();
        if (ObjectUtils.anyNull(moneyInfo.getTotalReceived(), moneyInfo.getNetPayment())) {
            log.warn(
                    "Order {} has no total received or net payment, start migrating data before recognizing the transactions",
                    order.getId()
            );
            var transactions = this.transactionRepository.findByOrderId(order.getId());
            order.recalculatePaymentState(transactions);
        }

        var refundResult = buildRefund(order, refundRequest);

        if (refundResult.refund.isEmpty() && CollectionUtils.isEmpty(refundResult.transactions)) {
            throw new ConstrainViolationException(
                    "refund_line_items",
                    ""
            );
        }

        order.addRefund(refundResult.refund);

        var refundTaxLineModel = recreateRefundTaxLines(refundResult.refund, order);
        order.updateOrInsertRefundTaxLine(refundTaxLineModel.getRight(), refundTaxLineModel.getKey());

        return refundResult;
    }

    private Pair<List<RefundTaxLine>, Map<Integer, BigDecimal>> recreateRefundTaxLines(Refund refund, Order order) {
        Map<Integer, BigDecimal> refundLineItemTaxMap = new LinkedHashMap<>();
        Map<Integer, BigDecimal> refundShippingTaxMap = new LinkedHashMap<>();

        if (!CollectionUtils.isEmpty(refund.getRefundLineItems())) {
            refundLineItemTaxMap = new RefundLineItemTax(
                    order.getRefundTaxLines(),
                    refund.getRefundLineItems(),
                    order.getLineItems()
            ).getResult();
        }

        if (!CollectionUtils.isEmpty(refund.getOrderAdjustments())) {
            var shippingAdjustments = refund.getOrderAdjustments().stream()
                    .filter(adjust -> adjust.getKind() == OrderAdjustment.RefundKind.shipping_refund)
                    .collect(Collectors.toSet());
            refundShippingTaxMap = new RefundShippingTax(
                    order.getRefundTaxLines(),
                    shippingAdjustments,
                    order.getShippingLines()
            ).getResult();
        }

        if (refundLineItemTaxMap.isEmpty() && refundShippingTaxMap.isEmpty()) {
            return Pair.of(List.of(), Map.of());
        }

        return createOrUpdateRefundTaxLineModel(refundLineItemTaxMap, refundShippingTaxMap, order);
    }

    private Pair<List<RefundTaxLine>, Map<Integer, BigDecimal>> createOrUpdateRefundTaxLineModel(
            Map<Integer, BigDecimal> refundLineItemTaxMap,
            Map<Integer, BigDecimal> refundShippingTaxMap,
            Order order
    ) {
        List<RefundTaxLine> refundTaxLines = new ArrayList<>();
        Map<Integer, BigDecimal> updatedRefundTaxLines;

        var refundingTaxLines = Stream
                .concat(
                        refundLineItemTaxMap.entrySet().stream(),
                        refundShippingTaxMap.entrySet().stream()
                )
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                Map.Entry::getValue,
                                BigDecimal::add
                        )
                ));

        var refundingTaxLineIds = refundingTaxLines.keySet();
        var refundedTaxLineIds = order.getRefundTaxLines().stream()
                .map(RefundTaxLine::getTaxLineId)
                .distinct()
                .toList();

        var newRefundTaxLines = refundingTaxLineIds.stream()
                .filter(taxLineId -> refundedTaxLineIds.stream().noneMatch(refundedTaxLineId -> Objects.equals(taxLineId, refundedTaxLineId)))
                .toList();
        if (!newRefundTaxLines.isEmpty()) {
            var refundTaxLineIds = this.orderIdGenerator.generateRefundTaxLineIds(newRefundTaxLines.size());
            refundTaxLines = createNewRefundTaxLines(refundTaxLineIds, newRefundTaxLines, refundingTaxLines);
        }

        updatedRefundTaxLines = refundingTaxLines.entrySet()
                .stream()
                .filter(entry -> !newRefundTaxLines.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        return Pair.of(refundTaxLines, updatedRefundTaxLines);
    }

    private List<RefundTaxLine> createNewRefundTaxLines(Deque<Integer> refundTaxLineIds, List<Integer> newRefundTaxLines, Map<Integer, BigDecimal> refundingTaxLines) {
        return newRefundTaxLines.stream()
                .map(taxLineId ->
                        new RefundTaxLine(
                                refundTaxLineIds.removeFirst(),
                                taxLineId,
                                refundingTaxLines.get(taxLineId)
                        ))
                .toList();
    }

    private final class RefundLineItemTax extends AbstractTaxMapBuilder<RefundLineItem, LineItem> {

        public RefundLineItemTax(List<RefundTaxLine> refundedTaxLines, Set<RefundLineItem> refundLineItems, List<LineItem> lineItems) {
            super(refundedTaxLines, refundLineItems, lineItems);
        }

        @Override
        protected Map<Integer, BigDecimal> reMapTax() {
            Map<Integer, BigDecimal> refundLineItemTax = new LinkedHashMap<>();
            for (var refundItem : this.refundItems) {
                final int lineItemId = refundItem.getLineItemId();

                var lineItem = this.originalItems.stream()
                        .filter(item -> item.getId() == lineItemId)
                        .findFirst()
                        .orElse(null);
                if (lineItem == null) continue;

                var effectiveTaxMap = CollectionUtils.isEmpty(lineItem.getTaxLines())
                        ? new LinkedHashMap<Integer, BigDecimal>()
                        : lineItem.getTaxLines().stream().collect(Collectors.toMap(TaxLine::getId, TaxLine::getPrice, (first, second) -> second, LinkedHashMap::new));

                if (!CollectionUtils.isEmpty(this.refundedTaxLines)) {
                    for (var rtl : this.refundedTaxLines) {
                        effectiveTaxMap.computeIfPresent(rtl.getTaxLineId(), (k, ov) -> ov.subtract(rtl.getAmount()));
                    }
                }

                for (var rl : refundLineItemTax.entrySet()) {
                    effectiveTaxMap.computeIfPresent(rl.getKey(), (key, ov) -> ov.subtract(rl.getValue()));
                }

                this.allocateTaxAmount(refundLineItemTax, refundItem.getTotalTax(), effectiveTaxMap);
            }
            return refundLineItemTax;
        }

    }

    private final class RefundShippingTax extends AbstractTaxMapBuilder<OrderAdjustment, ShippingLine> {
        public RefundShippingTax(List<RefundTaxLine> refundedTaxLines, Set<OrderAdjustment> refundShippingLines, List<ShippingLine> shippingLines) {
            super(refundedTaxLines, refundShippingLines, shippingLines);
        }

        @Override
        protected Map<Integer, BigDecimal> reMapTax() {
            var refundShippingTaxMap = new LinkedHashMap<Integer, BigDecimal>();

            var effectiveTaxMap = this.originalItems.stream()
                    .filter(shipping -> !CollectionUtils.isEmpty(shipping.getTaxLines()))
                    .flatMap(shipping -> shipping.getTaxLines().stream())
                    .collect(Collectors.toMap(TaxLine::getId, TaxLine::getPrice, (first, second) -> second, LinkedHashMap::new));

            if (!CollectionUtils.isEmpty(refundedTaxLines)) {
                for (var rl : refundedTaxLines) {
                    effectiveTaxMap.computeIfPresent(rl.getTaxLineId(), (k, ov) -> ov.subtract(rl.getAmount()));
                }
            }

            for (var refundItem : this.refundItems) {

                for (var rl : refundShippingTaxMap.entrySet()) {
                    effectiveTaxMap.computeIfPresent(rl.getKey(), (k, ov) -> ov.subtract(rl.getValue()));
                }

                this.allocateTaxAmount(refundShippingTaxMap, refundItem.getTaxAmount(), effectiveTaxMap);

            }

            return refundShippingTaxMap;
        }
    }

    private abstract static class AbstractTaxMapBuilder<T, R> {

        protected final List<RefundTaxLine> refundedTaxLines;
        protected final Set<T> refundItems;
        protected final List<R> originalItems;

        private final Map<Integer, BigDecimal> refundTaxMap;

        private AbstractTaxMapBuilder(List<RefundTaxLine> refundedTaxLines, Set<T> refundItems, List<R> originalItems) {
            this.refundedTaxLines = refundedTaxLines;
            this.refundItems = refundItems;
            this.originalItems = originalItems;

            this.refundTaxMap = reMapTax();
        }

        protected abstract Map<Integer, BigDecimal> reMapTax();

        protected void allocateTaxAmount(Map<Integer, BigDecimal> refundLineItemTax, BigDecimal totalTax, LinkedHashMap<Integer, BigDecimal> effectiveTaxMap) {
            for (Iterator<Integer> iterator = new LinkedList<>(effectiveTaxMap.keySet()).descendingIterator(); iterator.hasNext(); ) {
                final int taxLineId = iterator.next();

                BigDecimal remaining = effectiveTaxMap.get(taxLineId);
                if (!NumberUtils.isPositive(remaining)) {
                    continue;
                }

                var refundAmount = totalTax.min(remaining);
                totalTax = totalTax.subtract(refundAmount);

                refundLineItemTax.compute(taxLineId, (k, ov) -> ov == null ? refundAmount : ov.add(refundAmount));

                if (totalTax.compareTo(BigDecimal.ZERO) <= 0) {
                    break;
                }
            }
        }

        public Map<Integer, BigDecimal> getResult() {
            return refundTaxMap;
        }
    }

    private RefundResult buildRefund(Order order, RefundRequest refundRequest) {
        var suggestedRefund = this.refundCalculationService.calculateRefund(order, refundRequest);
        var refundLineItems = buildRefundItems(suggestedRefund);

        var refundTransactions = buildRefundTransactions(order, refundRequest.getTransactions());

        var orderAdjustments = buildOrderAdjustments(
                suggestedRefund, refundLineItems, refundTransactions, order);

        var processedAt = Instant.now();

        var refund = new Refund(
                this.orderIdGenerator.generateRefundId(),
                refundLineItems,
                orderAdjustments,
                refundRequest.getNote(),
                processedAt
        );

        return new RefundResult(refund, refundTransactions);
    }

    private Set<OrderAdjustment> buildOrderAdjustments(
            RefundCalculationResponse suggestedRefund,
            Set<RefundLineItem> refundLineItems,
            List<OrderTransactionCreateRequest> refundTransactions,
            Order order
    ) {
        var transactions = this.transactionRepository.findByOrderId(order.getId());

        var customerSpent = transactions.stream().anyMatch(OrderTransaction::isCaptureOrSale);

        var refundAllItems = this.isRefundAllItems(suggestedRefund, refundLineItems);

        if (!customerSpent) {
            if (!refundAllItems || CollectionUtils.isEmpty(order.getShippingLines())) return Set.of();
            var shippingRefund = this.createFullShippingRefund(order);
            return shippingRefund == null ? Set.of() : Set.of(shippingRefund);
        }

        var maxRefundable = suggestedRefund.getTransactions().stream()
                .map(RefundCalculationResponse.Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        var totalRefundingAmount = refundTransactions.stream()
                .map(OrderTransactionCreateRequest::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        var refundAllMoney = maxRefundable.compareTo(totalRefundingAmount) == 0;

        var isFullRefund = refundAllMoney && refundAllItems;
        if (isFullRefund) {
            var adjustments = new HashSet<OrderAdjustment>();

            var shippingRefund = this.createFullShippingRefund(order);
            if (shippingRefund != null) adjustments.add(shippingRefund);

            return Collections.unmodifiableSet(adjustments);
        }

        var adjustments = new HashSet<OrderAdjustment>();
        var suggestedShippingRefund = suggestedRefund.getShipping();
        if (NumberUtils.isPositive(suggestedShippingRefund.getAmount())) {
            var shippingAdjustment = this.createShippingRefund(suggestedShippingRefund, order.isTaxIncluded());
            adjustments.add(shippingAdjustment);
        }

        return Collections.unmodifiableSet(adjustments);
    }

    private OrderAdjustment createFullShippingRefund(Order order) {
        var shippingRefundRequest = new RefundRequest.Shipping().setFullRefund(true);
        var suggestedShippingRefund = this.refundCalculationService.suggestRefundShipping(order, shippingRefundRequest);
        if (NumberUtils.isPositive(suggestedShippingRefund.getAmount())) {
            return this.createShippingRefund(suggestedShippingRefund, order.isTaxIncluded());
        }
        return null;
    }

    private OrderAdjustment createShippingRefund(RefundCalculationResponse.Shipping suggestedShippingRefund, boolean taxIncluded) {
        var amount = suggestedShippingRefund.getAmount();
        var taxAmount = suggestedShippingRefund.getTax();
        if (taxIncluded && NumberUtils.isPositive(taxAmount)) {
            amount = amount.add(taxAmount);
        }
        var adjustmentId = this.orderIdGenerator.generateAdjustmentId();
        return new OrderAdjustment(
                adjustmentId,
                amount,
                taxAmount,
                OrderAdjustment.RefundKind.shipping_refund
        );
    }

    private boolean isRefundAllItems(RefundCalculationResponse suggestedRefund, Set<RefundLineItem> refundLineItems) {
        return suggestedRefund.getRefundableLineItems()
                .stream()
                .allMatch(line -> {
                    var refundedItem = refundLineItems.stream()
                            .filter(rf -> rf.getLineItemId() == line.getLineItemId())
                            .findFirst()
                            .orElse(null);
                    return refundedItem != null && refundedItem.getQuantity() == line.getMaximumRefundableQuantity();
                });
    }

    private List<OrderTransactionCreateRequest> buildRefundTransactions(
            Order order,
            List<OrderTransactionCreateRequest> inputTransactions
    ) {
        if (CollectionUtils.isEmpty(inputTransactions)) {
            return List.of();
        }

        var refundTransactionRequests = inputTransactions.stream()
                .filter(request -> {
                    if (!NumberUtils.isPositive(request.getAmount())) return false;

                    request.setKind(OrderTransaction.Kind.refund);
                    request.setStatus(OrderTransaction.Status.success);
                    return true;
                })
                .toList();

        if (refundTransactionRequests.isEmpty()) {
            return List.of();
        }

        var orderTransactions = this.transactionRepository.findByOrderId(order.getId());
        for (var refundTransaction : refundTransactionRequests) {
            var valid = true;
            if (!NumberUtils.isPositive(refundTransaction.getParentId())) {
                valid = false;
            } else {
                valid = orderTransactions.stream()
                        .anyMatch(transaction -> transaction.getId().getId() == refundTransaction.getParentId());
            }
            if (!valid) {
                throw new ConstrainViolationException(
                        "transactions",
                        ""
                );
            }
        }

        var requestedAmount = refundTransactionRequests.stream()
                .map(OrderTransactionCreateRequest::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (requestedAmount.compareTo(order.getMoneyInfo().getTotalReceived()) > 0) {
            throw new ConstrainViolationException(
                    "refund_amount",
                    ""
            );
        }

        return refundTransactionRequests;
    }

    private Set<RefundLineItem> buildRefundItems(RefundCalculationResponse suggestedRefund) {
        var suggestedItems = suggestedRefund.getRefundLineItems();
        if (CollectionUtils.isEmpty(suggestedItems)) {
            return Set.of();
        }
        var ids = this.orderIdGenerator.generateRefundTaxLineIds(suggestedItems.size());
        return suggestedItems.stream()
                .map(item ->
                        new RefundLineItem(ids.removeFirst(), item))
                .collect(Collectors.toSet());
    }

    record RefundResult(Refund refund, List<OrderTransactionCreateRequest> transactions) {
    }

    private Map<Integer, FFOLine> fetchFulfillmentOrders(OrderId orderId) {
        Map<Integer, FFOLine> results = new HashMap<>();

        this.fulfillmentOrderRepository.findByOrderId(orderId)
                .stream()
                .sorted(Comparator.comparing(FulfillmentOrder::getAssignedLocationId))
                .forEach(ffo -> {
                    var ffoItems = ffo.getLineItems();
                    if (CollectionUtils.isEmpty(ffoItems)) return;

                    ffoItems.forEach(item ->
                            results.computeIfAbsent(item.getLineItemId(), (id) -> new FFOLine(ffo.getAssignedLocationId(), new ArrayList<>()))
                                    .ffoItems().add(item));

                });

        return results;
    }

    private record FFOLine(
            int locationId,
            List<FulfillmentOrderLineItem> ffoItems
    ) {
    }

    private void createRefundRequests(
            List<RefundRequest.LineItem> refundItemRequests,
            OrderStagedChange.DecrementItem decrement,
            boolean shouldRestock,
            Map<Integer, FFOLine> fulfillmentLineItemMap) {
        final int lineItemId = decrement.getLineItemId();
        int refundItemQuantity = decrement.getDelta().intValue();

        if (!shouldRestock) {
            var itemRequest = new RefundRequest.LineItem();
            itemRequest.setLineItemId(lineItemId);
            itemRequest.setQuantity(refundItemQuantity);
            itemRequest.setRemoval(true);
            itemRequest.setRestockType(RefundLineItem.RestockType.no_restock);
            refundItemRequests.add(itemRequest);
            return;
        }

        var ffoLine = fulfillmentLineItemMap.get(lineItemId);
        if (ffoLine == null) {
            return;
        }

        int fulfillableQuantity = ffoLine.ffoItems.stream()
                .mapToInt(FulfillmentOrderLineItem::getRemainingQuantity)
                .sum();

        var request = new RefundRequest.LineItem();
        request.setLineItemId(lineItemId);
        request.setQuantity(refundItemQuantity);
        request.setRestockType(RefundLineItem.RestockType.cancel);
        request.setRemoval(true);

        request.setLocationId(ffoLine.locationId);
        request.setQuantity(Math.min(fulfillableQuantity, refundItemQuantity));

        refundItemRequests.add(request);
    }

    private @Nullable LineItem getQuantityReducibleLineItem(
            OrderStagedChange.DecrementItem decrement,
            Order order,
            HashMap<Integer, Integer> remainingQuantityMap,
            Warnings.Builder warningsBuilder
    ) {
        final int lineItemId = decrement.getLineItemId();
        Optional<LineItem> possiblyLineItem = findLineItem(order, lineItemId);
        if (possiblyLineItem.isEmpty()) {
            warningsBuilder.add(
                    "line_items",
                    "Can't find LineItem with id " + lineItemId
            );
            return null;
        }

        LineItem lineItem = possiblyLineItem.get();
        var remainingQuantity = remainingQuantityMap.get(lineItemId);
        if (remainingQuantity == null) {
            remainingQuantity = lineItem.getFulfillableQuantity();
        }
        remainingQuantity -= decrement.getDelta().intValue();
        if (remainingQuantity < 0) {
            warningsBuilder.add(
                    "refund",
                    "LineItem %s is not enough (require %s, available %s)"
                            .formatted(lineItemId, decrement.getDelta(), lineItem.getFulfillableQuantity())
            );
            return null;
        }
        remainingQuantityMap.put(lineItemId, remainingQuantity);
        return lineItem;
    }

    private Optional<LineItem> findLineItem(Order order, int lineItemId) {
        return order.getLineItems().stream()
                .filter(line -> line.getId() == lineItemId)
                .findFirst();
    }
}
