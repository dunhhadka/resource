package org.example.order.order.application.service.orderedit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.example.AdminClient;
import org.example.location.Location;
import org.example.location.LocationFilter;
import org.example.order.order.application.model.draftorder.TaxSetting;
import org.example.order.order.application.model.orderedit.request.OrderEditRequest;
import org.example.order.order.application.service.draftorder.TaxHelper;
import org.example.order.order.application.utils.NumberUtils;
import org.example.order.order.application.utils.OrderEditUtils;
import org.example.order.order.domain.edit.model.AddedLineItem;
import org.example.order.order.domain.edit.model.OrderEdit;
import org.example.order.order.domain.edit.model.OrderEditId;
import org.example.order.order.domain.edit.persistence.OrderEditRepository;
import org.example.order.order.domain.order.model.DiscountApplication;
import org.example.order.order.domain.order.model.LineItem;
import org.example.order.order.domain.order.model.Order;
import org.example.order.order.domain.order.model.OrderId;
import org.example.order.order.domain.order.persistence.OrderRepository;
import org.example.order.order.infrastructure.configuration.exception.ConstrainViolationException;
import org.example.order.order.infrastructure.data.dao.ProductDao;
import org.example.order.order.infrastructure.data.dto.ProductDto;
import org.example.order.order.infrastructure.data.dto.VariantDto;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEditContext {

    private final AdminClient adminClient;

    private final ProductDao productDao;

    private final OrderRepository orderRepository;
    private final OrderEditRepository orderEditRepository;

    private final TaxHelper taxHelper;


    private OrderEdit fetchOrderEdit(OrderEditId orderEditId) {
        var orderEdit = this.orderEditRepository.findById(orderEditId);
        if (orderEdit == null) {
            throw new ConstrainViolationException("order_edit", "not found with id = " + orderEditId);
        }
        if (orderEdit.isCommitted()) {
            throw new ConstrainViolationException("order_edit", "committed order edit can't edit");
        }
        return orderEdit;
    }

    private Order fetchOrder(int storeId, int orderId) {
        var order = this.orderRepository.findById(new OrderId(storeId, orderId));
        if (order == null) {
            throw new ConstrainViolationException(
                    "order",
                    "Order not found with id = " + orderId
            );
        }
        if (order.getCancelledOn() != null) {
            throw new ConstrainViolationException(
                    "order",
                    "Cancelled Order can't edit"
            );
        }
        return order;
    }

    public AddVariantsContext createContext(OrderEditId orderEditId, List<OrderEditRequest.AddVariant> requests) {
        return new AddVariantsContextImpl(orderEditId, requests);
    }

    public AddCustomItemContext createContext(OrderEditId orderEditId, OrderEditRequest.AddCustomItem request) {
        return new AddCustomItemContextImpl(orderEditId, request);
    }

    public SetQuantityContext<?> createContext(OrderEditId orderEditId, OrderEditRequest.SetItemQuantity request) {
        var lineItemIdPair = OrderEditUtils.parseLineItemId(request.getLineItemId());
        if (lineItemIdPair.getKey() != null) {
            return createSetQuantityContext(orderEditId, lineItemIdPair.getKey(), request);
        }
        return createSetQuantityContext(orderEditId, lineItemIdPair.getValue(), request);
    }

    private SetQuantityExistingLineContext createSetQuantityContext(OrderEditId orderEditId, Integer lineItemId, OrderEditRequest.SetItemQuantity request) {
        var orderEdit = this.fetchOrderEdit(orderEditId);
        var order = this.fetchOrder(orderEditId.getStoreId(), orderEdit.getOrderId());

        var lineItem = order.getLineItems().stream()
                .filter(line -> Objects.equals(line.getId(), lineItemId))
                .findFirst()
                .orElseThrow(() ->
                        new ConstrainViolationException(
                                "line_item",
                                "Line item not found with id %s".formatted(lineItemId)
                        ));

        BigDecimal currencyQuantity = BigDecimal.valueOf(lineItem.getFulfillableQuantity());
        BigDecimal requestedQuantity = request.getQuantity();

        if (requestedQuantity.compareTo(currencyQuantity) == 0) {
            return new ResetExistingItemContext(orderEdit, lineItem, request);
        } else if (requestedQuantity.compareTo(currencyQuantity) > 0) {
            return new IncreaseExistingItemContext(orderEdit, lineItem, request);
        } else {
            return new DecreaseExistingItemContext(orderEdit, lineItem, request);
        }
    }

    public SetItemDiscountContext createContext(OrderEditId orderEditId, OrderEditRequest.SetItemDiscount request) {
        return new SetItemDiscountImpl(orderEditId, request);
    }

    public record DiscountRequest(
            BigDecimal value,
            DiscountApplication.ValueType type,
            String description
    ) {

    }

    public final class SetItemDiscountImpl extends AbstractContext<OrderEditRequest.SetItemDiscount> implements SetItemDiscountContext {

        private final DiscountRequest discountRequest;
        private final TaxContext taxContext;
        private final AddedLineItem lineItem;

        public SetItemDiscountImpl(OrderEditId orderEditId, OrderEditRequest.SetItemDiscount request) {
            super(orderEditId, request);

            this.discountRequest = resolveDiscountRequest(request);

            UUID lineItemId = request.getLineItemId();
            this.lineItem = orderEdit().getLineItems().stream()
                    .filter(line -> line.getId().equals(lineItemId))
                    .findFirst()
                    .orElseThrow(() ->
                            new ConstrainViolationException(
                                    "line_item",
                                    "Line item not found with id = " + lineItemId
                            ));

            this.taxContext = this.resolveTaxContext(lineItem);
        }

        private TaxContext resolveTaxContext(AddedLineItem lineItem) {
            if (lineItem.isTaxable()) {
                Set<Integer> productIds = lineItem.getProductId() == null ? Set.of(0) : Set.of(lineItem.getProductId());
                return needTaxes(productIds);
            }
            return new TaxContext();
        }

        private DiscountRequest resolveDiscountRequest(OrderEditRequest.SetItemDiscount request) {
            var currency = orderEdit().getCurrency();
            if (NumberUtils.isPositive(request.getFixedValue())) {
                return new DiscountRequest(
                        request.getFixedValue().setScale(currency.getDefaultFractionDigits(), RoundingMode.UP),
                        DiscountApplication.ValueType.fixed_amount,
                        request.getDescription()
                );
            }
            if (NumberUtils.isPositive(request.getPercentValue())) {
                return new DiscountRequest(
                        request.getPercentValue(),
                        DiscountApplication.ValueType.percentage,
                        request.getDescription()
                );
            }
            throw new ConstrainViolationException(
                    "discount_request",
                    "Required discountValue for request"
            );
        }

        @Override
        public TaxContext taxContext() {
            return this.taxContext;
        }

        @Override
        public DiscountRequest discountRequest() {
            return discountRequest;
        }

        @Override
        public AddedLineItem lineItem() {
            return this.lineItem;
        }
    }

    public final class DecreaseExistingItemContext extends SetQuantityExistingLineContext {
        public DecreaseExistingItemContext(OrderEdit orderEdit, LineItem lineItem, OrderEditRequest.SetItemQuantity request) {
            super(orderEdit, lineItem, request);
        }
    }

    public final class IncreaseExistingItemContext extends SetQuantityExistingLineContext {
        public IncreaseExistingItemContext(OrderEdit orderEdit, LineItem lineItem, OrderEditRequest.SetItemQuantity request) {
            super(orderEdit, lineItem, request);
        }
    }

    public final class ResetExistingItemContext extends SetQuantityExistingLineContext {
        public ResetExistingItemContext(OrderEdit orderEdit, LineItem lineItem, OrderEditRequest.SetItemQuantity request) {
            super(orderEdit, lineItem, request);
        }
    }

    private SetQuantityAddedLineContext createSetQuantityContext(OrderEditId orderEditId, UUID lineItemId, OrderEditRequest.SetItemQuantity request) {
        var orderEdit = this.fetchOrderEdit(orderEditId);

        var lineItem = orderEdit.getLineItems().stream()
                .filter(line -> line.getId().equals(lineItemId))
                .findFirst()
                .orElseThrow(() ->
                        new ConstrainViolationException(
                                "line_item",
                                "Line item not found with id %s".formatted(lineItemId)
                        ));

        if (request.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
            return new RemovedAddedItemContext(orderEdit, lineItem, request);
        }
        return new AdjustAddedItemContext(orderEdit, lineItem, request);
    }

    public final class AdjustAddedItemContext extends AbstractContext<OrderEditRequest.SetItemQuantity> implements SetQuantityAddedLineContext, NeedTax {

        private final AddedLineItem lineItem;
        private final TaxContext taxContext;

        public AdjustAddedItemContext(OrderEdit orderEdit, AddedLineItem lineItem, OrderEditRequest.SetItemQuantity request) {
            super(orderEdit, request);

            this.lineItem = lineItem;
            if (!this.lineItem.isTaxable()) {
                this.taxContext = new TaxContext();
                return;
            }
            this.taxContext = needTaxes(Optional.ofNullable(this.lineItem.getProductId()).map(Set::of).orElse(Set.of(0)));
        }

        @Override
        public AddedLineItem lineItem() {
            return this.lineItem;
        }

        @Override
        public TaxContext taxContext() {
            return this.taxContext;
        }
    }

    public final class RemovedAddedItemContext extends AbstractContext<OrderEditRequest.SetItemQuantity> implements SetQuantityAddedLineContext, NeedTax {
        private final AddedLineItem lineItem;
        private final TaxContext taxContext;

        public RemovedAddedItemContext(OrderEdit orderEdit, AddedLineItem lineItem, OrderEditRequest.SetItemQuantity request) {
            super(orderEdit, request);

            this.lineItem = lineItem;
            if (!this.lineItem.isTaxable()) {
                taxContext = new TaxContext();
                return;
            }

            Set<Integer> productIds = lineItem.getProductId() == null ? Set.of(0) : Set.of(lineItem.getProductId());
            this.taxContext = needTaxes(productIds);
        }

        @Override
        public AddedLineItem lineItem() {
            return lineItem;
        }

        @Override
        public TaxContext taxContext() {
            return this.taxContext;
        }
    }

    public abstract class SetQuantityExistingLineContext extends AbstractContext<OrderEditRequest.SetItemQuantity> implements SetQuantityContext<LineItem>, NeedTax {

        private final LineItem lineItem;
        private final TaxContext taxContext;

        public SetQuantityExistingLineContext(OrderEdit orderEdit, LineItem lineItem, OrderEditRequest.SetItemQuantity request) {
            super(orderEdit, request);

            this.lineItem = lineItem;

            if (!this.lineItem.isTaxable()) {
                this.taxContext = new TaxContext();
                return;
            }
            Set<Integer> productIds = Optional.ofNullable(lineItem.getVariantInfo().getProductId()).map(Set::of).orElse(Set.of(0));
            this.taxContext = needTaxes(productIds);
        }

        @Override
        public LineItem lineItem() {
            return this.lineItem;
        }

        @Override
        public TaxContext taxContext() {
            return this.taxContext;
        }
    }

    // region public interface

    public interface SetItemDiscountContext extends BaseContext<OrderEditRequest.SetItemDiscount>, NeedTax {
        DiscountRequest discountRequest();

        AddedLineItem lineItem();
    }

    public interface SetQuantityAddedLineContext extends SetQuantityContext<AddedLineItem> {
        AddedLineItem lineItem();
    }

    public interface SetQuantityContext<L> extends BaseContext<OrderEditRequest.SetItemQuantity> {
        L lineItem();
    }

    public interface AddCustomItemContext extends NeedTax, BaseContext<OrderEditRequest.AddCustomItem> {
        Location getLocation();
    }

    public interface NeedTax {
        TaxContext taxContext();
    }

    public interface BaseContext<T> {
        OrderEdit orderEdit();

        T request();
    }

    public interface AddVariantsContext extends BaseContext<List<OrderEditRequest.AddVariant>>, NeedTax {
        VariantDto getVariantById(int variantId);

        ProductDto getProductById(int productId);

        Location getLocationById(Integer locationId);
    }

    // endregion public interface

    private final class AddCustomItemContextImpl extends AbstractContext<OrderEditRequest.AddCustomItem> implements AddCustomItemContext {

        private final TaxContext taxContext;
        private final Location location;

        public AddCustomItemContextImpl(OrderEditId orderEditId, OrderEditRequest.AddCustomItem request) {
            super(orderEditId, request);

            this.location = fetchLocation();

            if (!request.isTaxable()) {
                taxContext = new TaxContext();
                return;
            }

            taxContext = needTaxes(Set.of());
        }

        private Location fetchLocation() {
            boolean includeDefaultLocation = request().getLocationId() == null;
            var locations = this.needLocations(
                    includeDefaultLocation ? List.of() : List.of(request().getLocationId()),
                    includeDefaultLocation
            );
            return locations.get(request().getLocationId());
        }

        @Override
        public TaxContext taxContext() {
            return this.taxContext;
        }

        @Override
        public Location getLocation() {
            return null;
        }
    }

    private abstract class AbstractContext<T> implements BaseContext<T> {
        private final OrderEdit orderEdit;
        private final T request;
        private @Nullable Order order;

        protected AbstractContext(OrderEditId orderEditId, T request) {
            this.orderEdit = fetchOrderEdit(orderEditId);
            this.request = request;
        }

        protected AbstractContext(OrderEdit orderEdit, T request) {
            this.orderEdit = orderEdit;
            this.request = request;
        }

        @Override
        public OrderEdit orderEdit() {
            return this.orderEdit;
        }

        @Override
        public T request() {
            return this.request;
        }

        private int storeId() {
            return this.orderEdit.getId().getStoreId();
        }

        private Order needOrder() {
            if (this.order != null) return this.order;
            this.order = fetchOrder(storeId(), this.orderEdit.getOrderId());
            return this.order;
        }

        protected final VariantInfo needVariantInfo(List<Integer> variantIds) {
            if (CollectionUtils.isEmpty(variantIds)) {
                return new VariantInfo(Map.of(), Map.of());
            }

            var variants = productDao.findVariantByListId(storeId(), variantIds).stream()
                    .collect(Collectors.toMap(VariantDto::getId, Function.identity()));
            String variantsNotFound = variantIds.stream()
                    .filter(variantId -> !variants.containsKey(variantId))
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
            if (!variantsNotFound.isEmpty()) {
                throw new ConstrainViolationException(
                        "variants",
                        "Variants not found with id %s".formatted(variantsNotFound)
                );
            }

            var productIds = variants.values().stream()
                    .map(VariantDto::getProductId)
                    .distinct()
                    .toList();
            var products = productDao.findProductByListId(storeId(), productIds).stream()
                    .collect(Collectors.toMap(ProductDto::getId, Function.identity()));

            return new VariantInfo(variants, products);
        }

        protected final Map<Integer, Location> needLocations(List<Integer> locationIds, boolean includeDefaultLocation) {
            assert CollectionUtils.isNotEmpty(locationIds) || includeDefaultLocation : "Request for AddVariants is invalid";

            var filterBuilder = LocationFilter.builder();

            if (CollectionUtils.isNotEmpty(locationIds)) {
                filterBuilder.ids(locationIds);
            }
            if (includeDefaultLocation) filterBuilder.defaultLocation(true);

            var locationResponses = adminClient.locationFilter(storeId(), filterBuilder.build());

            var locationMap = locationResponses.stream()
                    .collect(Collectors.toMap(
                            location -> (int) location.getId(),
                            Function.identity(),
                            (location1, location2) -> onThrowWithDuplicate(location1, location2, locationResponses),
                            HashMap::new
                    ));

            if (CollectionUtils.isNotEmpty(locationIds)) {
                String locationNotFound = locationIds.stream()
                        .filter(locationId -> !locationMap.containsKey(locationId))
                        .map(String::valueOf)
                        .collect(Collectors.joining(","));
                if (!locationNotFound.isEmpty()) {
                    throw new ConstrainViolationException(
                            "locations",
                            "Locations not found with id = %s".formatted(locationNotFound)
                    );
                }
            }

            if (includeDefaultLocation) {
                var defaultLocations = locationResponses.stream()
                        .filter(Location::isDefaultLocation)
                        .toList();
                if (defaultLocations.isEmpty()) {
                    throw new IllegalStateException("Require one Default Location");
                }
                if (defaultLocations.size() > 1) {
                    log.info("Found more than one default location in {}", storeId());
                }
                locationMap.put(null, defaultLocations.get(0));
            }

            return locationMap;
        }

        private <T> T onThrowWithDuplicate(T location1, T location2, List<T> locationResponses) {
            if (log.isDebugEnabled()) {
                var locationsAsString = locationResponses.stream()
                        .map(Object::toString)
                        .collect(Collectors.joining(", \n"));

                log.debug(
                        """
                                [Duplicate Location Detected]
                                Fetched location list ({} items):
                                {}
                                Conflicting entries:
                                - Location1: {}
                                - Location2: {}
                                """,
                        locationResponses.size(),
                        locationsAsString,
                        location1,
                        location2
                );
            }

            throw new IllegalStateException("Duplicate Location ID found in location response: " + location1);
        }

        protected final TaxContext needTaxes(Set<Integer> productIds) {
            if (CollectionUtils.isEmpty(productIds)) {
                return new TaxContext();
            }

            productIds.add(0);

            String countryCode = OrderEditUtils.getCountryCode(needOrder());
            TaxSetting taxSetting = taxHelper.getTaxSetting(storeId(), countryCode, productIds, false);

            taxSetting = taxSetting.toBuilder().taxIncluded(needOrder().isTaxIncluded()).build();

            return new TaxContext(taxSetting);
        }

        protected record VariantInfo(Map<Integer, VariantDto> variants, Map<Integer, ProductDto> products) {
        }
    }

    private final class AddVariantsContextImpl extends AbstractContext<List<OrderEditRequest.AddVariant>> implements AddVariantsContext {

        private final Map<Integer, VariantDto> variants;
        private final Map<Integer, ProductDto> products;
        private final Map<Integer, Location> locations;
        private final TaxContext taxContext;

        public AddVariantsContextImpl(OrderEditId orderEditId, List<OrderEditRequest.AddVariant> requests) {
            super(orderEditId, requests);

            var variantInfo = this.fetchVariantInfo(requests);
            this.variants = variantInfo.variants;
            this.products = variantInfo.products;

            this.locations = this.fetchLocations(requests);

            this.taxContext = this.fetchTaxContext();
        }

        private TaxContext fetchTaxContext() {
            var productIdsNeedTax = this.variants.values().stream()
                    .filter(VariantDto::isTaxable)
                    .map(VariantDto::getProductId)
                    .filter(this.products::containsKey)
                    .collect(Collectors.toSet());

            return this.needTaxes(productIdsNeedTax);
        }

        private Map<Integer, Location> fetchLocations(List<OrderEditRequest.AddVariant> requests) {
            var locationIds = requests.stream()
                    .map(OrderEditRequest.AddVariant::getLocationId)
                    .filter(NumberUtils::isPositive)
                    .toList();

            boolean includeDefaultLocation = requests.size() != locationIds.size();

            return this.needLocations(locationIds, includeDefaultLocation);
        }

        private VariantInfo fetchVariantInfo(List<OrderEditRequest.AddVariant> requests) {
            var variantIds = requests.stream()
                    .map(OrderEditRequest.AddVariant::getVariantId)
                    .filter(NumberUtils::isPositive)
                    .distinct()
                    .toList();
            return this.needVariantInfo(variantIds);
        }

        @Override
        public VariantDto getVariantById(int variantId) {
            return this.variants.get(variantId);
        }

        @Override
        public ProductDto getProductById(int productId) {
            return this.products.get(productId);
        }

        @Override
        public Location getLocationById(Integer locationId) {
            return this.locations.get(locationId);
        }

        @Override
        public TaxContext taxContext() {
            return this.taxContext;
        }
    }
}
