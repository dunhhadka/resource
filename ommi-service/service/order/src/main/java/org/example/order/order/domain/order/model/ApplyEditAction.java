package org.example.order.order.domain.order.model;

import org.apache.commons.lang3.StringUtils;
import org.example.order.order.application.service.orderedit.AddService;
import org.example.order.order.domain.edit.model.AddedDiscountAllocation;
import org.example.order.order.domain.edit.model.AddedDiscountApplication;
import org.example.order.order.domain.order.persistence.OrderIdGenerator;

import java.math.BigDecimal;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public sealed interface ApplyEditAction permits Order {

    Order order();

    default List<LineItem> addNewLineItems(AddService.AddModel addModel) {
        var newLineItems = addModel.newLineItems();
        var possiblyDiscountMap = addModel.discountMap();

        this.internalAddLineItems(newLineItems);

        this.applyDiscounts(possiblyDiscountMap);

        var totalAddedLineItemPrice = newLineItems.stream()
                .map(LineItem::getOriginalTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var increaseAmount = newLineItems.stream()
                .map(LineItem::getDiscountedTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        adjustTotalPrice(totalAddedLineItemPrice, increaseAmount);

        orderModified();

        return newLineItems;
    }

    void orderModified();

    default void adjustTotalPrice(BigDecimal lineItemPrice, BigDecimal increaseAmount) {
        MoneyInfo moneyInfo = order().getMoneyInfo();

        var builder = moneyInfo.toBuilder()
                .totalLineItemPrice(moneyInfo.getTotalLineItemPrice().add(lineItemPrice))

                .subtotalPrice(moneyInfo.getSubtotalPrice().add(increaseAmount))
                .currentSubtotalPrice(moneyInfo.getCurrentSubtotalPrice().add(increaseAmount))

                .totalPrice(moneyInfo.getTotalPrice().add(increaseAmount))
                .currentTotalPrice(moneyInfo.getCurrentTotalPrice().add(increaseAmount))

                .unpaidAmount(moneyInfo.getUnpaidAmount().add(increaseAmount))
                .totalOutstanding(moneyInfo.getTotalOutstanding().add(increaseAmount));

        var totalDiscount = lineItemPrice.subtract(increaseAmount);
        if (totalDiscount.signum() > 0) {
            builder
                    .totalDiscounts(moneyInfo.getTotalDiscounts().add(totalDiscount))
                    .currentTotalDiscounts(moneyInfo.getCurrentTotalDiscounts().add(totalDiscount));
        }

        order().setMoneyInfo(builder.build());
    }

    default void applyDiscounts(Map<Integer, AddService.AddDiscountModel> possiblyDiscountMap) {
        OrderIdGenerator idGenerator = order().getOrderIdGenerator();

        int discountSize = possiblyDiscountMap.size();
        var allocationIds = idGenerator.generateDiscountAllocationIds(discountSize);
        var applicationIds = idGenerator.generateDiscountApplicationIds(discountSize);

        var lineItemMap = order().getLineItems().stream()
                .collect(Collectors.toMap(LineItem::getId, Function.identity()));

        possiblyDiscountMap.forEach((lineItemId, discountModel) -> {
            if (discountModel.isEmpty()) return;

            var lineItem = lineItemMap.get(lineItemId);
            assert lineItem != null;

            DiscountApplication application = createNewApplication(discountModel.application(), applicationIds);
            application.setAggRoot(order());
            order().getDiscountApplications().add(application);

            BigDecimal allocateAmount = discountModel.allocation().getAmount();
            lineItem.allocateDiscountAmount(allocateAmount);

            int applicationIndex = order().getDiscountApplications().size();
            DiscountAllocation allocation = createNewAllocations(
                    discountModel.allocation(), allocationIds,
                    lineItemId, application, applicationIndex);
            lineItem.addAllocation(allocation);
        });
    }

    default DiscountAllocation createNewAllocations(
            AddedDiscountAllocation allocation,
            Deque<Integer> allocationIds,
            Integer lineItemId,
            DiscountApplication application,
            int applicationIndex) {
        return new DiscountAllocation(
                allocationIds.removeFirst(),
                allocation.getAmount(),
                lineItemId,
                DiscountAllocation.TargetType.line_item,
                application.getId(),
                applicationIndex
        );
    }

    default DiscountApplication createNewApplication(AddedDiscountApplication application, Deque<Integer> applicationIds) {
        return new DiscountApplication(
                applicationIds.removeFirst(),
                application.getValue(),
                application.getValueType(),
                DiscountApplication.TargetType.line_item,
                DiscountApplication.RuleType.product,
                StringUtils.EMPTY,
                "Title",
                application.getDescription()
        );
    }

    default void internalAddLineItems(List<LineItem> lineItems) {
        lineItems.forEach(lineItem -> lineItem.setAggRoot(order()));

        order().getLineItems().addAll(lineItems);

        increaseTotalWeight(lineItems.stream().mapToInt(line -> line.getVariantInfo().getGrams()).sum());
    }

    default void increaseTotalWeight(int totalWeight) {
        int newValue = order().getTotalWeight() + totalWeight;
        order().setTotalWeight(newValue);
    }
}
