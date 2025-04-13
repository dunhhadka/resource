package org.example.order.order.application.service.order;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.example.AdminClient;
import org.example.location.Location;
import org.example.location.LocationFilter;
import org.example.order.order.infrastructure.configuration.exception.ConstrainViolationException;
import org.example.order.order.infrastructure.configuration.exception.UserError;
import org.example.order.order.application.model.refund.request.RefundRequest;
import org.example.order.order.application.model.refund.response.RefundCalculationResponse;
import org.example.order.order.application.utils.NumberUtils;
import org.example.order.order.domain.fulfillmentorder.model.FulfillmentOrder;
import org.example.order.order.domain.fulfillmentorder.persistence.FulfillmentOrderRepository;
import org.example.order.order.domain.order.model.*;
import org.example.order.order.domain.refund.model.OrderAdjustment;
import org.example.order.order.domain.refund.model.Refund;
import org.example.order.order.domain.refund.model.RefundLineItem;
import org.example.order.order.domain.transaction.model.OrderTransaction;
import org.example.order.order.domain.transaction.persistence.TransactionRepository;
import org.example.order.order.infrastructure.data.dao.ProductDao;
import org.example.order.order.infrastructure.data.dto.VariantDto;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class RefundCalculationService {

    private final AdminClient adminClient;

    private final ProductDao productDao;
    private final FulfillmentOrderRepository fulfillmentOrderRepository;
    private final TransactionRepository transactionRepository;

    public RefundCalculationResponse calculateRefund(Order order, RefundRequest refundRequest) {
        var shipping = suggestRefundShipping(order, refundRequest.getShipping());
        var refundItemResult = suggestRefundLineItems(order, refundRequest);

        var transactionResult = suggestRefundTransactions(order, refundItemResult.lineItems, shipping);

        return RefundCalculationResponse.builder()
                .shipping(shipping)
                .refundLineItems(refundItemResult.lineItems)
                .refundableLineItems(refundItemResult.refundableLineItems)
                .transactions(transactionResult.getRight())
                .build();
    }

    private Pair<BigDecimal, List<RefundCalculationResponse.Transaction>> suggestRefundTransactions(
            Order order,
            List<RefundCalculationResponse.LineItem> refundItems,
            RefundCalculationResponse.Shipping shipping
    ) {
        BigDecimal totalRequestRefundAmount;
        if (order.isTaxIncluded() || !NumberUtils.isPositive(order.getMoneyInfo().getTotalTax())) {
            totalRequestRefundAmount = refundItems.stream()
                    .map(RefundCalculationResponse.LineItem::getSubtotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .add(shipping.getAmount());
        } else {
            totalRequestRefundAmount = refundItems.stream()
                    .map(line -> line.getSubtotal().add(line.getTotalTax()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .add(shipping.getAmount())
                    .add(shipping.getTax());
        }

        BigDecimal availableAmount = BigDecimal.ZERO;
        var suggestedTransactions = new ArrayList<RefundCalculationResponse.Transaction>();
        var refundableTransactions = getRefundableTransactions(order);

        return null;
    }

    private List<RefundCalculationResponse.Transaction> getRefundableTransactions(Order order) {
        var transactions = this.transactionRepository.findByOrderId(order.getId());
        var successCaptureAndSaleTransactions = transactions.stream()
                .filter(OrderTransaction::isCaptureOrSale)
                .toList();
        var availableRefundTransactions = new ArrayList<RefundCalculationResponse.Transaction>();

        return availableRefundTransactions;
    }

    private RefundItemResult suggestRefundLineItems(Order order, RefundRequest refundRequest) {
        var requestLines = safeSelect(refundRequest.getRefundLineItems(), line -> line.getQuantity() > 0);

        if (CollectionUtils.isEmpty(requestLines)) {
            if (refundRequest.getOption().isCreate()) {
                var refundableLineItems = getRefundableLineItems(order);
                return new RefundItemResult(refundableLineItems, List.of());
            }
            return RefundItemResult.EMPTY;
        }

        return suggestRefundItems(order, requestLines, refundRequest.getOption());
    }

    private RefundItemResult suggestRefundItems(Order order, List<RefundRequest.LineItem> requestLines, RefundRequest.Option option) {
        var refundableLineItems = getRefundableLineItems(order);

        forceRestockType(order.getId(), refundableLineItems, requestLines);

        validateRefundItems(order.getLineItems(), refundableLineItems, requestLines);

        validateLocation(order.getId(), requestLines, option);

        if (option.isCreate()) {
            requestLines = chooseRefundTypeForLineItem(order, refundableLineItems, requestLines);
        }

        var refundItems = calculateRefundLineItemInfo(order, refundableLineItems, requestLines);

        return new RefundItemResult(refundableLineItems, refundItems);
    }

    private List<RefundCalculationResponse.LineItem> calculateRefundLineItemInfo(
            Order order,
            List<RefundCalculationResponse.LineItem> refundableLineItems,
            List<RefundRequest.LineItem> requestLines
    ) {
        var calculationResult = new ArrayList<RefundCalculationResponse.LineItem>();
        var cachedRefundQuantityMap = new HashMap<Integer, Integer>();
        for (var refundLineItem : requestLines) {
            var refundableLineItem = refundableLineItems.stream()
                    .filter(item -> item.getLineItemId() == refundLineItem.getLineItemId())
                    .findFirst()
                    .orElseThrow();

            var lineItem = refundableLineItem.getOrderLineItem();
            var suggestQuantity = Math.min(refundableLineItem.getMaximumRefundableQuantity(), refundLineItem.getQuantity());
            var refundedQuantity = lineItem.getQuantity() - refundableLineItem.getMaximumRefundableQuantity();

            var refundCachedQuantity = cachedRefundQuantityMap.getOrDefault(lineItem.getId(), 0);
            refundedQuantity += refundCachedQuantity;

            var lineItemDiscountDetails = categorizeLineItemDiscount(lineItem, order);
            var totalProductDiscount = lineItemDiscountDetails.getRight();
            var totalOrderDiscount = lineItemDiscountDetails.getLeft();

            var totalTax = lineItem.getTaxLines().stream()
                    .map(TaxLine::getPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            var calculatedRefundLine = refundableLineItem.copy();
            calculationResult.add(calculatedRefundLine);

            // set quantity
            calculatedRefundLine.setQuantity(suggestQuantity);

            calculatedRefundLine
                    .setLocationId(refundLineItem.getLocationId())
                    .setRestockType(refundLineItem.getRestockType())
                    .setRemoval(refundLineItem.isRemoval());

            // chia thành 2 case
            var isRefundAllItem = suggestQuantity == calculatedRefundLine.getMaximumRefundableQuantity();
            if (isRefundAllItem && refundedQuantity == 0) {
                this.suggestRefundPrice(
                        calculatedRefundLine,
                        lineItem.getPrice(),
                        totalProductDiscount,
                        totalOrderDiscount,
                        totalTax,
                        lineItem.getQuantity()
                );
            } else {
                var roundingAccuracy = order.getMoneyInfo().getCurrency().getDefaultFractionDigits();
                var suggestTotalTax = suggestRefundAmount(
                        totalTax, roundingAccuracy,
                        lineItem.getQuantity(), refundedQuantity,
                        suggestQuantity
                );
                var suggestOrderDiscount = suggestRefundAmount(
                        totalOrderDiscount, roundingAccuracy,
                        lineItem.getQuantity(), refundedQuantity,
                        suggestQuantity
                );
                var suggestProductDiscount = suggestRefundAmount(
                        totalProductDiscount, roundingAccuracy,
                        lineItem.getQuantity(), refundedQuantity,
                        suggestQuantity
                );

                this.suggestRefundPrice(
                        calculatedRefundLine,
                        lineItem.getPrice(),
                        suggestProductDiscount,
                        suggestOrderDiscount,
                        suggestTotalTax,
                        suggestQuantity
                );
            }

            cachedRefundQuantityMap.compute(lineItem.getId(), (key, oldValue) -> oldValue == null ? suggestQuantity : oldValue + suggestQuantity);
        }

        return calculationResult;
    }

    private BigDecimal suggestRefundAmount(
            BigDecimal amount, int roundingAccuracy,
            int totalQuantity, int refundedQuantity, int suggestQuantity
    ) {
        return suggestRefundAmount(
                amount, roundingAccuracy,
                totalQuantity, refundedQuantity, suggestQuantity, RoundingStyle.last_n
        );
    }

    private BigDecimal suggestRefundAmount(
            BigDecimal amount, int roundingAccuracy,
            int totalQuantity, int refundedQuantity,
            int suggestQuantity, RoundingStyle style
    ) {
        var totalAmountL = roundingAccuracy == 0
                ? amount.longValue()
                : amount.movePointRight(roundingAccuracy).longValue();
        long subtotalAmountL = subtotalWithRounding(totalAmountL, totalQuantity, refundedQuantity, suggestQuantity, style);
        if (roundingAccuracy == 0) return BigDecimal.valueOf(subtotalAmountL);
        return BigDecimal.valueOf(subtotalAmountL).movePointLeft(roundingAccuracy);
    }

    private long subtotalWithRounding(long totalAmountL, int totalQuantity, int refundedQuantity, int suggestQuantity, RoundingStyle style) {
        var remain = totalAmountL % totalQuantity;
        if (remain == 0) {
            return (totalAmountL / totalQuantity) * suggestQuantity;
        }

        return (totalAmountL / totalQuantity) * suggestQuantity;
    }

    enum RoundingStyle {
        last_n,
        first_n
    }

    private void suggestRefundPrice(
            RefundCalculationResponse.LineItem calculatedRefundLine,
            BigDecimal lineItemPrice,
            BigDecimal totalProductDiscount,
            BigDecimal totalOrderDiscount,
            BigDecimal totalTax,
            int suggestedQuantity
    ) {
        var quantity = BigDecimal.valueOf(suggestedQuantity);
        var discountSubtotal = lineItemPrice.multiply(quantity).subtract(totalProductDiscount);
        var discountedUnitPrice = discountSubtotal.divide(quantity, RoundingMode.FLOOR);
        var subtotal = discountSubtotal.subtract(totalOrderDiscount);

        calculatedRefundLine
                .setSubtotal(subtotal)
                .setTotalTax(totalTax)
                .setTotalCartDiscount(totalOrderDiscount)
                .setDiscountedPrice(discountedUnitPrice)
                .setDiscountedSubtotal(discountSubtotal);
    }

    private Pair<BigDecimal, BigDecimal> categorizeLineItemDiscount(LineItem lineItem, Order order) {
        BigDecimal productDiscount = BigDecimal.ZERO;
        BigDecimal orderDiscount = BigDecimal.ZERO;
        if (CollectionUtils.isNotEmpty(lineItem.getDiscountAllocations())) {
            var discountApplications = order.getDiscountApplications();
            for (var allocation : lineItem.getDiscountAllocations()) {
                if (isProductDiscount(allocation, discountApplications)) {
                    productDiscount = productDiscount.add(allocation.getAmount());
                } else {
                    orderDiscount = orderDiscount.add(allocation.getAmount());
                }
            }
        }
        return Pair.of(productDiscount, orderDiscount);
    }

    private boolean isProductDiscount(DiscountAllocation allocation, List<DiscountApplication> discountApplications) {
        var application = discountApplications.get(allocation.getApplicationIndex());
        return application.getRuleType() == DiscountApplication.RuleType.product;
    }

    private List<RefundRequest.LineItem> chooseRefundTypeForLineItem(
            Order order,
            List<RefundCalculationResponse.LineItem> refundableLineItems,
            List<RefundRequest.LineItem> requestLines
    ) {
        var refundedDetails = getRefundedQuantityDetail(order.getRefunds());
        var restockState = refundableLineItems.stream()
                .collect(Collectors.toMap(
                        RefundCalculationResponse.LineItem::getLineItemId,
                        (item) -> {
                            var removableQuantity = item.getOrderLineItem().getFulfillableQuantity();

                            var refunded = refundedDetails.get(item.getLineItemId());
                            if (refunded != null) {
                                removableQuantity = Math.min(
                                        item.getOrderLineItem().getFulfillableQuantity(),
                                        item.getMaximumRefundableQuantity()
                                );
                            }

                            return new RestockItemContext(
                                    item.getMaximumRefundableQuantity(),
                                    removableQuantity,
                                    item.getMaximumRefundableQuantity() - removableQuantity
                            );
                        }
                ));

        var processedRequests = new ArrayList<RefundRequest.LineItem>();
        for (var refundItemRequest : requestLines) {
            var state = restockState.get(refundItemRequest.getLineItemId());
            switch (refundItemRequest.getRestockType()) {
                case no_restock -> {
                    var splitLineItems = splitLineItem(refundItemRequest, state);
                    processedRequests.add(refundItemRequest);
                    processedRequests.addAll(splitLineItems);
                }
                case cancel -> {
                    refundItemRequest.setRemoval(true);
                    state.reduce(refundItemRequest.getQuantity(), true);
                    processedRequests.add(refundItemRequest);
                }
                case _refund -> {
                    refundItemRequest.setRemoval(false);
                    var splitLineItems = splitLineItem(refundItemRequest, state);
                    processedRequests.addAll(splitLineItems);
                }
            }
        }
        return processedRequests;
    }

    private List<RefundRequest.LineItem> splitLineItem(RefundRequest.LineItem original, RestockItemContext state) {
        var refundLineItems = new ArrayList<RefundRequest.LineItem>();
        var remainingQuantity = state.reduce(original.getQuantity(), original.isRemoval());
        if (remainingQuantity > 0) {
            var returnRequest = original.toBuilder()
                    .removal(original.isRemoval())
                    .quantity(remainingQuantity)
                    .build();
            refundLineItems.add(returnRequest);
        }
        if (remainingQuantity < original.getQuantity()) {
            var splitRestock = switch (original.getRestockType()) {
                case no_restock -> RefundLineItem.RestockType.no_restock;
                case _refund -> RefundLineItem.RestockType.cancel;
                case cancel -> RefundLineItem.RestockType._refund;
            };
            var splitQuantity = state.reduce(original.getQuantity() - remainingQuantity, !original.isRemoval());
            var cancelRequest = original.toBuilder()
                    .restockType(splitRestock)
                    .removal(!original.isRemoval())
                    .quantity(splitQuantity)
                    .build();
            refundLineItems.add(cancelRequest);
        }
        return refundLineItems;
    }

    @Getter
    static class RestockItemContext {
        private int remaining;
        private int cancelable;
        private int returnable;

        public RestockItemContext(int remaining, int cancelable, int returnable) {
            this.remaining = remaining;
            this.cancelable = cancelable;
            this.returnable = returnable;
        }

        public int reduce(int quantity, boolean removal) {
            if (removal) {
                if (this.cancelable == 0) return 0;
                var toCancel = Math.min(this.cancelable, quantity);
                this.remaining -= toCancel;
                this.cancelable -= toCancel;
                return toCancel;
            } else {
                if (this.returnable == 0) return 0;
                var toReturn = Math.min(this.returnable, quantity);
                this.remaining -= toReturn;
                this.returnable -= toReturn;
                return toReturn;
            }
        }
    }

    private Map<Integer, RefundedItemContext> getRefundedQuantityDetail(Set<Refund> refunds) {
        if (CollectionUtils.isEmpty(refunds)) {
            return Map.of();
        }
        var contexts = new HashMap<Integer, RefundedItemContext>();
        for (var refund : refunds) {
            if (CollectionUtils.isEmpty(refund.getRefundLineItems())) {
                continue;
            }
            for (var refundLineItem : refund.getRefundLineItems()) {
                var lineItemId = refundLineItem.getLineItemId();
                var context = contexts.get(lineItemId);
                if (context == null) {
                    context = new RefundedItemContext(refundLineItem);
                    contexts.put(lineItemId, context);
                    continue;
                }
                context.addItem(refundLineItem);
            }
        }
        return contexts;
    }

    /**
     * Get thông tin của các lineItem đã refund trước đó
     * - refunded: LineItem đã từng refund
     * - removed: LineItem đã tửng remove
     * - returned: LineItem đã từng return
     */
    @Getter
    static class RefundedItemContext {
        private int refunded;
        private int removed;
        private int returned;

        public RefundedItemContext(RefundLineItem refundLineItem) {
            addItem(refundLineItem);
        }

        /**
         * - cancel: Nếu đơn hàng chưa fulfill xong thì khách hàng trả hàng
         * - return: Nếu đơn hàng đã fulfill, sau đó khách hàng mới trả hàng
         */
        public void addItem(RefundLineItem refundLineItem) {
            this.refunded += refundLineItem.getQuantity();
            switch (refundLineItem.getType()) {
                case cancel -> this.removed += refundLineItem.getQuantity(); //
                case _refund -> this.returned += refundLineItem.getQuantity();
                default -> {
                }
            }
        }
    }

    private void validateLocation(
            OrderId orderId,
            List<RefundRequest.LineItem> requestLines,
            RefundRequest.Option option
    ) {
        var fulfillmentOrders = this.fulfillmentOrderRepository.findByOrderId(orderId);
        var ffoLocationIds = fulfillmentOrders.stream()
                .map(FulfillmentOrder::getAssignedLocationId)
                .filter(NumberUtils::isPositive)
                .distinct()
                .toList();
        var inputLocations = requestLines.stream()
                .map(RefundRequest.LineItem::getLocationId)
                .filter(NumberUtils::isPositive)
                .distinct()
                .toList();
        var filterLocationIds = Stream.concat(ffoLocationIds.stream(), inputLocations.stream()).distinct().toList();
        if (filterLocationIds.isEmpty()) {
            return;
        }

        var locationFilter = LocationFilter.builder().ids(filterLocationIds).build();
        var locationMap = this.adminClient.locationFilter(orderId.getStoreId(), locationFilter).stream()
                .collect(Collectors.toMap(location -> (int) location.getId(), Function.identity()));

        if (!inputLocations.isEmpty()) {
            var noneExistLocation = inputLocations.stream().anyMatch(id -> !locationMap.containsKey(id));
            if (noneExistLocation) {
                throw new ConstrainViolationException("location", "location not exist");
            }
        }

        for (var refundItemRequest : requestLines) {
            Integer defaultLocationId;
            if (refundItemRequest.getLocationId() == null) {
                final int lineItemId = refundItemRequest.getLineItemId();
                defaultLocationId = fulfillmentOrders.stream()
                        .filter(ffo -> ffo.getLineItems().stream().anyMatch(line -> line.getLineItemId() == lineItemId))
                        .map(FulfillmentOrder::getAssignedLocationId)
                        .findFirst()
                        .orElse(null);
            } else {
                defaultLocationId = refundItemRequest.getLocationId();
            }
            var restockLocation = filterRestockLocation(locationMap, defaultLocationId);
            if (restockLocation == null) {
                throw new ConstrainViolationException("location", "require location");
            }
            refundItemRequest.setLocationId((int) restockLocation.getId());
        }
    }

    private Location filterRestockLocation(Map<Integer, Location> locationMap, Integer locationId) {
        if (NumberUtils.isPositive(locationId)) {
            var defaultLocation = locationMap.get(locationId);
            if (defaultLocation != null) return defaultLocation;
        }

        var locations = locationMap.values().stream().toList();
        return locations.stream()
                .filter(Location::isDefaultLocation)
                .findFirst()
                .orElse(locations.get(0));
    }

    private void validateRefundItems(
            List<LineItem> lineItems,
            List<RefundCalculationResponse.LineItem> refundableLineItems,
            List<RefundRequest.LineItem> requestLines
    ) {
        var requestedRefundItemMap = reduceToRefundQuantityPerLineItem(requestLines);
        for (var refundItemEntry : requestedRefundItemMap.entrySet()) {
            var lineItemId = refundItemEntry.getKey();

            boolean noneExistLine = lineItems.stream().noneMatch(line -> line.getId() == lineItemId);
            if (noneExistLine) {
                throw new ConstrainViolationException(
                        "refund_line_item",
                        "cannot be blank"
                );
            }

            var refundableLineItem = refundableLineItems.stream()
                    .filter(line -> line.getLineItemId() == lineItemId)
                    .findFirst()
                    .orElseThrow(() ->
                            new ConstrainViolationException(
                                    "line_item",
                                    "require refundable line item"
                            ));
            var validModel = refundItemEntry.getValue();
            if (validModel.getQuantity() > refundableLineItem.getMaximumRefundableQuantity()) {
                throw new ConstrainViolationException(
                        "refund_line_item",
                        "cannot refund more items than were purchased"
                );
            }
            if (validModel.getRemoveQuantity() > refundableLineItem.getOrderLineItem().getFulfillableQuantity()) {
                throw new ConstrainViolationException(
                        "refund_line_item",
                        "cannot remove more than the fulfillable quantity"
                );
            }
        }
    }

    private Map<Integer, RefundItemValidationModel> reduceToRefundQuantityPerLineItem(List<RefundRequest.LineItem> requestLines) {
        Map<Integer, RefundItemValidationModel> models = new LinkedHashMap<>();
        for (var requestLine : requestLines) {
            var lineItemId = requestLine.getLineItemId();
            var model = models.get(lineItemId);
            if (model == null) {
                model = new RefundItemValidationModel(requestLine);
                models.put(lineItemId, model);
                continue;
            }
            model.addItem(requestLine);
        }
        return models;
    }

    @Getter
    public static class RefundItemValidationModel {
        private int quantity;
        private int removeQuantity;

        public RefundItemValidationModel(RefundRequest.LineItem requestLine) {
            this.addItem(requestLine);
        }

        public void addItem(RefundRequest.LineItem requestLine) {
            this.quantity += requestLine.getQuantity();
            if (requestLine.isRemoval()) {
                removeQuantity += requestLine.getQuantity();
            }
        }
    }

    private void forceRestockType(
            OrderId orderId,
            List<RefundCalculationResponse.LineItem> refundableLineItems,
            List<RefundRequest.LineItem> requestLines
    ) {
        if (CollectionUtils.isEmpty(refundableLineItems)) {
            return;
        }

        forceDefaultRestock(requestLines);
        forceRestockTypeWithProductCondition(orderId.getStoreId(), requestLines, refundableLineItems);
    }

    private void forceRestockTypeWithProductCondition(
            int storeId,
            List<RefundRequest.LineItem> requestLines,
            List<RefundCalculationResponse.LineItem> refundableLineItems
    ) {
        var lineItemMap = refundableLineItems.stream()
                .map(RefundCalculationResponse.LineItem::getOrderLineItem)
                .collect(Collectors.toMap(LineItem::getId, Function.identity()));

        List<Integer> variantIds = new ArrayList<>();
        List<Pair<Integer, RefundRequest.LineItem>> lineItemVariants = new ArrayList<>();

        for (var refundLineItem : requestLines) {
            if (refundLineItem.getRestockType() == RefundLineItem.RestockType.no_restock) {
                continue;
            }
            var orderLineItem = lineItemMap.get(refundLineItem.getLineItemId());
            if (orderLineItem == null) continue;

            // if restock_type != no_restock
            if (orderLineItem.getVariantInfo().isProductExists()
                    && NumberUtils.isPositive(orderLineItem.getVariantInfo().getVariantId())
            ) {
                variantIds.add(orderLineItem.getVariantInfo().getVariantId());

                lineItemVariants.add(Pair.of(orderLineItem.getVariantInfo().getVariantId(), refundLineItem));
            } else {
                // default custom variant => set restock_type = no_restock
                refundLineItem.setRestockType(RefundLineItem.RestockType.no_restock);
            }
        }

        if (!variantIds.isEmpty()) {
            var variants = this.productDao.findVariantByListId(storeId, variantIds).stream()
                    .collect(Collectors.toMap(VariantDto::getId, Function.identity()));

            lineItemVariants
                    .forEach(pair -> {
                        var variantId = pair.getKey();
                        var variant = variants.get(variantId);
                        if (variant == null) {
                            var lineItem = pair.getValue();
                            lineItem.setRestockType(RefundLineItem.RestockType.no_restock);
                        }
                    });
        }
    }

    private void forceDefaultRestock(List<RefundRequest.LineItem> requestLines) {
        if (CollectionUtils.isEmpty(requestLines)) {
            return;
        }
        for (var refundLineItem : requestLines) {
            if (refundLineItem.getRestockType() == null) {
                refundLineItem.setRestockType(RefundLineItem.RestockType.no_restock);
            }
            switch (refundLineItem.getRestockType()) {
                case _refund -> refundLineItem.setRemoval(false);
                case cancel -> refundLineItem.setRemoval(true);
            }
        }
    }

    /**
     * Những line không thể refund
     * - lineItem có fulfillmentStatus = restocked
     * - lineItem có currencyQuantity = originalQuantity - refundedQuantity == 0
     */
    private List<RefundCalculationResponse.LineItem> getRefundableLineItems(Order order) {
        List<RefundCalculationResponse.LineItem> refundableLineItems = new ArrayList<>();

        var refundedQuantityLineMap = getRefundedQuantityLineMap(order);

        for (var orderLineItem : order.getLineItems()) {
            if (LineItem.FulfillmentStatus.restocked.equals(orderLineItem.getFulfillmentStatus())) {
                continue;
            }

            var lineItemId = orderLineItem.getId();
            var originalQuantity = orderLineItem.getQuantity();
            var refundedQuantity = refundedQuantityLineMap.getOrDefault(lineItemId, 0);

            if (originalQuantity > refundedQuantity) {
                var refundLineItem = RefundCalculationResponse.LineItem.builder()
                        .maximumRefundableQuantity(originalQuantity - refundedQuantity)
                        .lineItemId(lineItemId)
                        .price(orderLineItem.getPrice())
                        .originalPrice(orderLineItem.getPrice())
                        .orderLineItem(orderLineItem)
                        .build();
                refundableLineItems.add(refundLineItem);
            }
        }

        return refundableLineItems;
    }

    private Map<Integer, Integer> getRefundedQuantityLineMap(Order order) {
        if (CollectionUtils.isEmpty(order.getRefunds())) {
            return Map.of();
        }
        return order.getRefunds().stream()
                .filter(refund -> CollectionUtils.isNotEmpty(refund.getRefundLineItems()))
                .flatMap(refund -> refund.getRefundLineItems().stream())
                .collect(Collectors.groupingBy(
                        RefundLineItem::getLineItemId,
                        Collectors.reducing(
                                0,
                                RefundLineItem::getQuantity,
                                Integer::sum
                        )
                ));
    }

    private static <T> List<T> safeSelect(List<T> list, Predicate<T> condition) {
        if (CollectionUtils.isEmpty(list)) {
            return List.of();
        }
        return list.stream()
                .filter(condition)
                .toList();
    }

    record RefundItemResult(
            List<RefundCalculationResponse.LineItem> refundableLineItems,
            List<RefundCalculationResponse.LineItem> lineItems) {

        public static final RefundItemResult EMPTY = new RefundItemResult(List.of(), List.of());

    }

    // region shipping
    public RefundCalculationResponse.Shipping suggestRefundShipping(Order order, RefundRequest.Shipping shippingRequest) {
        var shipping = new RefundCalculationResponse.Shipping();

        var totalShippingPrice = BigDecimal.ZERO;
        var shippingTax = BigDecimal.ZERO;
        var shippingRefunded = BigDecimal.ZERO;
        var shippingRefundedTax = BigDecimal.ZERO;

        if (CollectionUtils.isNotEmpty(order.getShippingLines())) {
            for (var shippingLine : order.getShippingLines()) {
                totalShippingPrice = totalShippingPrice.add(shippingLine.getPrice());
                if (CollectionUtils.isEmpty(shippingLine.getTaxLines())) {
                    continue;
                }

                var totalShippingLineTax = shippingLine.getTaxLines().stream()
                        .map(TaxLine::getPrice)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                shippingTax = shippingTax.add(totalShippingLineTax);
            }

            if (CollectionUtils.isNotEmpty(order.getRefunds())) {
                var orderAdjustments = order.getRefunds().stream()
                        .filter(refund -> CollectionUtils.isNotEmpty(refund.getOrderAdjustments()))
                        .flatMap(refund -> refund.getOrderAdjustments().stream())
                        .filter(adjustment -> adjustment.getKind() == OrderAdjustment.RefundKind.shipping_refund)
                        .toList();
                for (var adjustment : orderAdjustments) {
                    shippingRefunded = shippingRefunded.add(adjustment.getAmount());
                    if (order.isTaxIncluded()) {
                        shippingRefunded = shippingRefunded.add(adjustment.getTaxAmount());
                    }
                    shippingRefundedTax = shippingRefundedTax.add(adjustment.getTaxAmount());
                }
            }

            var maximumRefundShipping = totalShippingPrice.subtract(shippingRefunded);
            shipping.setMaximumRefundable(maximumRefundShipping);
        }

        if (shippingRequest != null) {
            var currency = order.getMoneyInfo().getCurrency();

            var refundShippingPrice = BigDecimal.ZERO;
            if (NumberUtils.isPositive(shippingRequest.getAmount())) {
                refundShippingPrice = shippingRequest.getAmount()
                        .setScale(currency.getDefaultFractionDigits(), RoundingMode.FLOOR);
            } else if (BooleanUtils.isTrue(shippingRequest.getFullRefund())) {
                refundShippingPrice = shipping.getMaximumRefundable();
            }

            int compareRefundPrice = refundShippingPrice.compareTo(shipping.getMaximumRefundable());
            if (compareRefundPrice > 0) {
                throw new ConstrainViolationException(UserError.builder()
                        .message("refund price must be less than or equal to maximum shipping price")
                        .fields(List.of("shipping"))
                        .build());
            }
            if (compareRefundPrice == 0) {
                shipping.setAmount(refundShippingPrice);
                shipping.setTax(shippingTax.subtract(shippingRefundedTax));
            } else {
                var refundTaxAmount = refundShippingPrice.multiply(shippingTax)
                        .divide(totalShippingPrice, currency.getDefaultFractionDigits(), RoundingMode.FLOOR);
                shipping.setAmount(refundShippingPrice);
                shipping.setTax(refundTaxAmount);
            }
        }

        return shipping.setTax(shipping.getTax().stripTrailingZeros())
                .setAmount(shipping.getAmount().stripTrailingZeros())
                .setMaximumRefundable(shipping.getMaximumRefundable().stripTrailingZeros());
    }

    // endregion shipping
}
