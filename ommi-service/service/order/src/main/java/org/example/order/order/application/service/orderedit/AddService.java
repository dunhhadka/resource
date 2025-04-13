package org.example.order.order.application.service.orderedit;

import com.google.common.base.Verify;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.example.location.Location;
import org.example.order.order.application.utils.NumberUtils;
import org.example.order.order.application.utils.OrderEditUtils;
import org.example.order.order.domain.edit.model.*;
import org.example.order.order.domain.order.model.LineItem;
import org.example.order.order.domain.order.model.Order;
import org.example.order.order.domain.order.model.VariantInfo;
import org.example.order.order.domain.order.persistence.OrderIdGenerator;
import org.example.order.order.infrastructure.data.dao.ProductDao;
import org.example.order.order.infrastructure.data.dto.ProductDto;
import org.example.order.order.infrastructure.data.dto.VariantDto;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class AddService {

    private final ProductDao productDao;
    private final OrderIdGenerator orderIdGenerator;

    public List<LineItem> addItems(
            Order order,
            OrderEdit orderEdit,
            OrderEditUtils.GroupedStagedChange changes,
            Map<Integer, Location> locations,
            Warnings.Builder warningsBuilder
    ) {
        var addVariantRequests = changes.addVariants();
        var addCustomItemRequests = changes.addCustomItems();
        int itemCount = addVariantRequests.size() + addCustomItemRequests.size();

        if (itemCount == 0) return List.of();
        var lineItemIds = this.orderIdGenerator.generateLineItemIds(itemCount);

        if (log.isDebugEnabled()) {
            log.debug("Adding {} new items for Order with id {}", itemCount, order.getId());
        }

        int storeId = order.getId().getStoreId();
        var productVariantInfo = fetchedAddedVariants(storeId, addVariantRequests);

        var newLineItemWithIds = Stream
                .concat(
                        addCustomItemRequests.stream()
                                .map(request ->
                                        new LineItemWithId(
                                                request.getLineItemId(),
                                                createNewLineItem(request, locations, lineItemIds))),
                        addVariantRequests.stream()
                                .map(request ->
                                        new LineItemWithId(
                                                request.getLineItemId(),
                                                createNewLineItem(request, productVariantInfo, locations, lineItemIds, warningsBuilder))))
                .filter(lineItemWithId -> lineItemWithId.lineItem.isPresent())
                .toList();

        if (newLineItemWithIds.isEmpty()) return List.of();

        var addModel = buildAddModel(newLineItemWithIds, changes.addItemDiscounts(), orderEdit, warningsBuilder);

        return order.addNewLineItems(addModel);
    }

    private AddModel buildAddModel(
            List<LineItemWithId> newLineItemWithIds,
            List<OrderStagedChange.AddItemDiscount> addItemDiscounts,
            OrderEdit orderEdit,
            Warnings.Builder warningsBuilder
    ) {
        var newLineItems = newLineItemWithIds.stream()
                .map(lineItemWithId -> lineItemWithId.lineItem)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
        if (CollectionUtils.isEmpty(addItemDiscounts)) {
            return new AddModel(newLineItems, Map.of());
        }

        var addedItemMap = orderEdit.getLineItems().stream()
                .collect(Collectors.toMap(AddedLineItem::getId, Function.identity()));

        Map<Integer, AddDiscountModel> discountMap = new LinkedHashMap<>();
        for (var lineItemWithId : newLineItemWithIds) {
            var addedItemId = lineItemWithId.addedItemId;
            var lineItem = lineItemWithId.lineItem;
            if (lineItem.isEmpty()) {
                continue;
            }
            var addedItem = addedItemMap.get(addedItemId);

            Verify.verify(addedItem != null);

            if (!addedItem.isHasStagedDiscount()) {
                discountMap.put(lineItem.get().getId(), AddDiscountModel.empty());
                continue;
            }

            var discount = getDiscountModel(addedItemId, orderEdit, warningsBuilder);
            if (discount.isEmpty()) {
                discountMap.put(lineItem.get().getId(), AddDiscountModel.empty());
            } else {
                discountMap.put(lineItem.get().getId(), discount.get());
            }
        }

        return new AddModel(newLineItems, discountMap);
    }

    private Optional<AddDiscountModel> getDiscountModel(UUID addedItemId, OrderEdit orderEdit, Warnings.Builder warningsBuilder) {
        var possiblyAllocation = orderEdit.getDiscountAllocations().stream()
                .filter(allocation -> allocation.getLineItemId().equals(addedItemId))
                .findFirst();
        if (possiblyAllocation.isEmpty()) {
            warningsBuilder.add("discount allocation", "require allocation this here");
            return Optional.empty();
        }

        var allocation = possiblyAllocation.get();
        var application = orderEdit.getDiscountApplications().stream()
                .filter(da -> da.getId().equals(allocation.getApplicationId()))
                .findFirst()
                .orElse(null);
        if (application == null) {
            warningsBuilder.add("discount", "application not found for allocation " + allocation.getApplicationId());
            return Optional.empty();
        }

        return Optional.of(new AddDiscountModel(allocation, application));
    }

    public record AddModel(
            List<LineItem> newLineItems,
            Map<Integer, AddDiscountModel> discountMap
    ) {
    }

    public record AddDiscountModel(
            AddedDiscountAllocation allocation,
            AddedDiscountApplication application
    ) {
        public static AddDiscountModel empty() {
            return new AddDiscountModel(null, null);
        }

        public boolean isEmpty() {
            return this.allocation == null && this.application == null;
        }
    }

    private Optional<LineItem> createNewLineItem(
            OrderStagedChange.AddVariant request,
            ProductVariantInfo productVariantInfo,
            Map<Integer, Location> locations,
            Deque<Integer> lineItemIds,
            Warnings.Builder warningsBuilder
    ) {
        int variantId = request.getVariantId();
        var variant = productVariantInfo.variants.get(variantId);
        if (variant == null) {
            warningsBuilder.add("variant", "variant not found by id = " + variantId);
            return Optional.empty();
        }

        var product = productVariantInfo.products.get(variant.getProductId());
        if (product == null) {
            warningsBuilder.add("product", "product not found by id = " + variant.getProductId());
            return Optional.empty();
        }

        return Optional.of(
                new LineItem(
                        lineItemIds.removeFirst(),
                        request.getQuantity().intValue(),
                        variant.getPrice(),
                        buildVariantInfo(product, variant),
                        variant.isTaxable()
                ).withLocationId(locations.get(request.getLocationId()))
        );
    }

    private VariantInfo buildVariantInfo(ProductDto product, VariantDto variant) {
        return VariantInfo.builder()
                .variantId(variant.getId())
                .productId(product.getId())
                .productExists(true)
                .name(product.getName())
                .title(product.getName())
                .variantTitle(variant.getTitle())
                .vendor(product.getVendor())
                .sku(variant.getSku())
                .grams(variant.getGrams())
                .requireShipping(variant.isRequiresShipping())
                .inventoryManagement(variant.getInventoryManagement())
                .restockable(true)
                .inventoryItemId(variant.getInventoryItemId())
                .unit(variant.getUnit())
                .build();
    }

    private Optional<LineItem> createNewLineItem(
            OrderStagedChange.AddCustomItem request,
            Map<Integer, Location> locations,
            Deque<Integer> lineItemIds
    ) {
        return Optional.of(
                new LineItem(
                        lineItemIds.removeFirst(),
                        request.getQuantity().intValue(),
                        request.getOriginalUnitPrice(),
                        VariantInfo.builder()
                                .title(request.getTitle())
                                .requireShipping(request.isRequireShipping())
                                .restockable(false)
                                .build(),
                        request.isTaxable()
                ).withLocationId(locations.get(request.getLocationId())));
    }

    private record LineItemWithId(
            UUID addedItemId,
            Optional<LineItem> lineItem
    ) {
    }

    private ProductVariantInfo fetchedAddedVariants(int storeId, List<OrderStagedChange.AddVariant> addVariantRequests) {
        if (CollectionUtils.isEmpty(addVariantRequests)) {
            return new ProductVariantInfo(Map.of(), Map.of());
        }

        var variantIds = addVariantRequests.stream()
                .map(OrderStagedChange.AddVariant::getVariantId)
                .filter(NumberUtils::isPositive)
                .distinct().toList();
        var variants = this.productDao.findVariantByListId(storeId, variantIds)
                .stream().collect(Collectors.toMap(VariantDto::getId, Function.identity()));

        var productIds = variants.values().stream()
                .map(VariantDto::getProductId)
                .distinct().toList();
        var products = this.productDao.findProductByListId(storeId, productIds)
                .stream().collect(Collectors.toMap(ProductDto::getId, Function.identity()));

        return new ProductVariantInfo(products, variants);
    }

    private record ProductVariantInfo(
            Map<Integer, ProductDto> products,
            Map<Integer, VariantDto> variants
    ) {
    }
}
