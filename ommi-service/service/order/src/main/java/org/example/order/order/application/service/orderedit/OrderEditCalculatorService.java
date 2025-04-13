package org.example.order.order.application.service.orderedit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Streams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.order.application.model.orderedit.response.*;
import org.example.order.order.application.utils.JsonUtils;
import org.example.order.order.application.utils.OrderEditUtils;
import org.example.order.order.domain.edit.model.OrderEditId;
import org.example.order.order.domain.edit.model.OrderStagedChange;
import org.example.order.order.domain.order.model.DiscountAllocation;
import org.example.order.order.domain.order.model.TaxLine;
import org.example.order.order.infrastructure.data.dao.*;
import org.example.order.order.infrastructure.data.dto.OrderDto;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEditCalculatorService {

    private final OrderDao orderDao;
    private final LineItemDao lineItemDao;
    private final DiscountApplicationDao applicationDao;
    private final DiscountAllocationDao allocationDao;
    private final TaxLineDao taxLineDao;
    private final RefundTaxLineDao refundTaxLineDao;

    private final OrderEditDao orderEditDao;
    private final OrderEditLineItemDao editLineItemDao;
    private final OrderEditDiscountApplicationDao editDiscountApplicationDao;
    private final OrderEditDiscountAllocationDao editDiscountAllocationDao;
    private final OrderEditTaxLineDao editTaxLineDao;
    private final OrderEditStagedChangeDao stagedChangeDao;

    /**
     * Get toàn bộ DTO của order, orderEdit sau đó join 2 phần này lại với nhau
     */
    public CalculatedOrder calculateOrder(OrderEditId orderEditId) {

        EntityGraph entityGraph = this.fetchEntityGraph(orderEditId);

        EditingContext context = this.buildContext(entityGraph);

        CalculatedOrder calculatedOrder = this.mapToCalculatedOrder(entityGraph.editGraph.orderEdit);

        List<BuilderSteps.Result> addedItems = entityGraph.editGraph.lineItems.stream()
                .map(line -> buildAddedLineItem(line, context))
                .toList();

        List<CalculatedLineItem> addedLineItems = addedItems.stream().map(BuilderSteps.Result::lineItem).toList();
        calculatedOrder.setAddedLineItems(addedLineItems);

        List<BuilderSteps.Result> existItems = entityGraph.orderGraph.lineItems.stream()
                .map(line -> buildExistLineItem(line, context))
                .toList();

        List<CalculatedLineItem> exitingLineItems = existItems.stream().map(BuilderSteps.Result::lineItem).toList();
        calculatedOrder.setLineItems(exitingLineItems);

        List<CalculatedLineItem> fulfilledItems = exitingLineItems.stream()
                .filter(line -> line.getEditableQuantity().compareTo(BigDecimal.ZERO) == 0)
                .toList();
        calculatedOrder.setFulfilledLineItems(fulfilledItems);

        List<CalculatedLineItem> unFulfilledItems = exitingLineItems.stream()
                .filter(line -> line.getQuantity().compareTo(BigDecimal.ZERO) > 0 && line.getEditableQuantity().compareTo(BigDecimal.ZERO) > 0)
                .toList();
        calculatedOrder.setUnfulfilledLineItems(unFulfilledItems);

        List<OrderStagedChangeModel> stagedChanges = entityGraph.editGraph.stagedChanges.stream()
                .map(OrderEditMapper::map)
                .toList();
        calculatedOrder.setStagedChanges(stagedChanges);

        List<CalculatedDiscountApplication> discountApplications = entityGraph.editGraph.editDiscountApplications.stream()
                .map(CalculatedDiscountApplication::new)
                .toList();
        calculatedOrder.setAddedDiscountApplications(discountApplications);

        List<CalculatedTaxLine> taxLines = Streams
                .concat(
                        addedItems.stream().map(BuilderSteps.Result::taxLines),
                        existItems.stream().map(BuilderSteps.Result::taxLines),
                        Stream.ofNullable(context.shippingTaxes)
                ).collect(MergedTaxLine.mergedMaps());
        calculatedOrder.setTaxLines(taxLines);

        return calculatedOrder;
    }

    private BuilderSteps.Result buildExistLineItem(LineItemDto lineItem, EditingContext context) {
        int lineItemId = lineItem.getId();

        var quantityChange = context.quantityChanges.get(lineItemId);
        var quantityAction = OrderEditUtils.resolveQuantityAction(quantityChange);

        return LineItemBuilder.forLineItem(
                        new LineItemBuilder.Context(
                                lineItem,
                                quantityAction,
                                context.existingTaxes.get(lineItemId),
                                context.newTaxLines.get(String.valueOf(lineItemId)),
                                context.existedAllocations.get(lineItemId)
                        ))
                .build();
    }

    private EditingContext buildContext(EntityGraph entityGraph) {

        OrderEditUtils.GroupedStagedChange changes = OrderEditUtils.groupOrderStagedChange(entityGraph.editGraph.stagedChanges);

        // for added lineItem
        // Map to [Key: AddedLineItemId] -> [Value: AddLineItemAction]
        var addedActions = changes.addLineItemActionsStream()
                .collect(Collectors.toMap(OrderStagedChange.AddLineItemAction::getLineItemId, Function.identity()));

        // Map to [Key: AddedLineItemId] -> [Value: AddItemDiscount]
        var addedDiscounts = changes.addItemDiscounts().stream()
                .collect(Collectors.toMap(OrderStagedChange.AddItemDiscount::getLineItemId, Function.identity()));

        // Map to [Key: AddedLineItemId] -> [Value: OrderEditDiscountAllocationDto[]]
        var allocations = entityGraph.editGraph.editDiscountAllocations.stream()
                .collect(Collectors.groupingBy(OrderEditDiscountAllocationDto::getLineItemId));

        // Map to [Key: AddedLineItemId/ExistingLineItemId] -> [Value: OrderEditTaxLine[]]
        var newTaxLines = entityGraph.editGraph.editTaxLines.stream()
                .collect(Collectors.groupingBy(OrderEditTaxLineDto::getTargetId));

        // For ExistingItem
        var quantityChanges = changes.quantityAdjustmentsStream()
                .collect(Collectors.toMap(OrderStagedChange.QuantityAdjustment::getLineItemId, Function.identity()));

        // Map to [Key: ExistingLineItemId] -> [Value: DiscountAllocation[]]
        var existedAllocations = entityGraph.orderGraph.discountAllocations.stream()
                .filter(discount -> discount.getTargetType() == DiscountAllocation.TargetType.line_item)
                .collect(Collectors.groupingBy(DiscountAllocationDto::getTargetId));

        // Map to [Key: ExistingTaxLineId] -> [Value: RefundTaxLine[]]
        var refundTaxMap = entityGraph.orderGraph.refundTaxLines.stream()
                .collect(Collectors.groupingBy(RefundTaxLineDto::getTaxLineId));

        // Map to [Key: ExistingLineItemId] -> [Value: ExistingTaxContext[]], ExistingTaxContext: {TaxLineDto, RefundTaxLineDto[]}
        var existingTaxes = entityGraph.orderGraph.taxLines.stream()
                .filter(taxLine -> taxLine.getTargetType() == TaxLine.TargetType.line_item)
                .collect(Collectors.groupingBy(
                        TaxLineDto::getTargetId,
                        Collectors.mapping(
                                taxLine -> new LineItemBuilder.ExistingTaxContext(taxLine, refundTaxMap.getOrDefault(taxLine.getId(), List.of())),
                                Collectors.toList())
                ));

        var shippingTaxes = entityGraph.orderGraph.taxLines.stream()
                .filter(taxLine -> taxLine.getTargetType() == TaxLine.TargetType.shipping)
                .collect(MergedTaxLine.mergeToMap());

        return new EditingContext(
                addedActions,
                quantityChanges,
                addedDiscounts,
                allocations,
                newTaxLines,
                existedAllocations,
                existingTaxes,
                shippingTaxes
        );
    }

    private BuilderSteps.Result buildAddedLineItem(OrderEditLineItemDto lineItem, EditingContext context) {
        var lineItemId = lineItem.getId();

        var addAction = context.addedActions.get(lineItemId);
        var addDiscount = context.addedDiscounts.get(lineItemId);
        var changes = new AddedLineItemBuilder.Changes(addAction, addDiscount);

        return AddedLineItemBuilder.forLineItem(
                        lineItem,
                        new AddedLineItemBuilder.Context(
                                changes,
                                context.newTaxLines.get(lineItemId.toString()),
                                context.allocations.get(lineItemId)
                        ))
                .build();
    }


    /**
     * Có 2 đối tượng cần build là AddedLineItem và LineItem
     */

    private CalculatedOrder mapToCalculatedOrder(OrderEditDto source) {
        CalculatedOrder target = new CalculatedOrder();

        target.setId(source.getId());
        target.setStoreId(source.getStoreId());
        target.setOrderId(source.getOrderId());

        target.setCommitted(source.isCommitted());

        target.setSubtotalLineItemQuantity(source.getSubtotalLineItemQuantity());

        target.setSubtotalPrice(source.getSubtotalPrice());
        target.setCartDiscountAmount(source.getCartDiscountAmount());
        target.setTotalPrice(source.getTotalPrice());
        target.setTotalOutstanding(source.getTotalOutstanding());

        return target;
    }

    private EntityGraph fetchEntityGraph(OrderEditId orderEditId) {
        var editGraph = fetchEditGraph(orderEditId);

        int storeId = editGraph.orderEdit.getStoreId();
        int orderId = editGraph.orderEdit.getOrderId();
        var orderGraph = fetchOrderGraph(storeId, orderId);

        return new EntityGraph(orderGraph, editGraph);
    }

    private OrderGraph fetchOrderGraph(int storeId, int orderId) {
        return new OrderGraph(
                this.orderDao.getById(storeId, orderId),
                this.lineItemDao.getByOrderId(storeId, orderId),
                this.applicationDao.getByOrderId(storeId, orderId),
                this.allocationDao.getByOrderId(storeId, orderId),
                this.taxLineDao.getByOrderId(storeId, orderId),
                this.refundTaxLineDao.getByOrderId(storeId, orderId)
        );
    }

    private OrderEditGraph fetchEditGraph(OrderEditId orderEditId) {
        int storeId = orderEditId.getStoreId();
        int editId = orderEditId.getId();

        return new OrderEditGraph(
                this.orderEditDao.getById(storeId, editId),
                this.editLineItemDao.getByOrderEditId(storeId, editId),
                this.editDiscountApplicationDao.getByOrderEditId(storeId, editId),
                this.editDiscountAllocationDao.getByOrderEditId(storeId, editId),
                this.editTaxLineDao.getByOrderEditId(storeId, editId),
                this.stagedChangeDao.getByOrderEditId(storeId, editId)
                        .stream()
                        .map(this::convert)
                        .toList()
        );
    }

    private OrderStagedChange convert(OrderEditStagedChangeDto changeDto) {
        try {
            String changeValue = changeDto.getValue();

            OrderStagedChange.BaseAction action = JsonUtils.unmarshal(changeValue, OrderStagedChange.BaseAction.class);

            return new OrderStagedChange(changeDto.getId(), changeDto.getType(), action);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    // region record

    record EditingContext(
            Map<UUID, OrderStagedChange.AddLineItemAction> addedActions,
            Map<Integer, OrderStagedChange.QuantityAdjustment> quantityChanges,
            Map<UUID, OrderStagedChange.AddItemDiscount> addedDiscounts,
            Map<UUID, List<OrderEditDiscountAllocationDto>> allocations,
            Map<String, List<OrderEditTaxLineDto>> newTaxLines,
            Map<Integer, List<DiscountAllocationDto>> existedAllocations,
            Map<Integer, List<LineItemBuilder.ExistingTaxContext>> existingTaxes,
            Map<MergedTaxLine.TaxLineKey, MergedTaxLine> shippingTaxes) {

    }

    record EntityGraph(
            OrderGraph orderGraph,
            OrderEditGraph editGraph
    ) {
    }

    record OrderGraph(
            OrderDto order,
            List<LineItemDto> lineItems,
            List<DiscountApplicationDto> discountApplications,
            List<DiscountAllocationDto> discountAllocations,
            List<TaxLineDto> taxLines,
            List<RefundTaxLineDto> refundTaxLines
    ) {
    }

    record OrderEditGraph(
            OrderEditDto orderEdit,
            List<OrderEditLineItemDto> lineItems,
            List<OrderEditDiscountApplicationDto> editDiscountApplications,
            List<OrderEditDiscountAllocationDto> editDiscountAllocations,
            List<OrderEditTaxLineDto> editTaxLines,
            List<OrderStagedChange> stagedChanges
    ) {

    }

    // endregion record
}
