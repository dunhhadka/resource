package org.example.order.order.application.service.order;

import com.google.common.collect.Lists;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.example.AdminClient;
import org.example.customer.Customer;
import org.example.location.Location;
import org.example.order.order.application.model.orderedit.request.OrderEditRequest;
import org.example.order.order.application.service.orderedit.OrderEditCommitService;
import org.example.order.order.domain.edit.model.OrderEdit;
import org.example.order.order.infrastructure.configuration.exception.ConstrainViolationException;
import org.example.order.order.infrastructure.configuration.exception.UserError;
import org.example.order.order.application.common.SupportedCurrencies;
import org.example.order.order.application.model.fulfillmentorder.OrderRoutingResponse;
import org.example.order.order.application.model.order.request.CustomAttributeRequest;
import org.example.order.order.application.model.order.request.OrderCreateRequest;
import org.example.order.order.application.model.order.request.OrderTransactionCreateRequest;
import org.example.order.order.application.model.order.response.OrderPaymentResult;
import org.example.order.order.application.model.refund.request.RefundRequest;
import org.example.order.order.application.model.refund.response.RefundCalculationResponse;
import org.example.order.order.application.service.customer.CustomerService;
import org.example.order.order.application.utils.AddressHelper;
import org.example.order.order.application.utils.BigDecimals;
import org.example.order.order.application.utils.CustomerPhoneUtils;
import org.example.order.order.application.utils.NumberUtils;
import org.example.order.order.domain.order.model.*;
import org.example.order.order.domain.order.persistence.OrderIdGenerator;
import org.example.order.order.domain.order.persistence.OrderRepository;
import org.example.order.order.domain.payment.persistence.PaymentRepository;
import org.example.order.order.domain.refund.model.OrderAdjustment;
import org.example.order.order.domain.refund.model.Refund;
import org.example.order.order.domain.refund.model.RefundLineItem;
import org.example.order.order.domain.refund.model.RefundTaxLine;
import org.example.order.order.domain.transaction.model.OrderTransaction;
import org.example.order.order.domain.transaction.persistence.TransactionRepository;
import org.example.order.order.infrastructure.data.dao.OrderDao;
import org.example.order.order.infrastructure.data.dao.ProductDao;
import org.example.order.order.infrastructure.data.dao.StoreDao;
import org.example.order.order.infrastructure.data.dto.ProductDto;
import org.example.order.order.infrastructure.data.dto.StoreDto;
import org.example.order.order.infrastructure.data.dto.VariantDto;
import org.example.product.OrderRouting;
import org.example.product.OrderRoutingRequest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderWriteService {

    private final OrderIdGenerator orderIdGenerator;

    private final StoreDao storeDao;
    private final OrderDao orderDao;
    private final ProductDao productDao;

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final TransactionRepository transactionRepository;

    private final OrderMapper orderMapper;

    private final RefundCalculationService refundCalculationService;
    private final CustomerService customerService;
    private final OrderEditCommitService orderEditCommitService;

    private final AdminClient adminClient;

    private final ApplicationEventPublisher applicationEventPublisher;
    private final MessageSource messageSource;

    @Transactional
    public OrderId createOrder(int storeId, OrderCreateRequest orderRequest) {
        //TODO: requireNonNull authorDetail

        var store = this.findStoreById(storeId);

        var currency = this.resolveCurrency(orderRequest.getCurrency(), store);

        var sourceInfo = mapSources(orderRequest.getSource(), orderRequest.getSourceName());
        var reference = buildReference(storeId, orderRequest);
        var trackingInfo = buildTrackingInfo(sourceInfo, orderRequest, reference);
        var appChannelPair = buildAppAndChannel(storeId, sourceInfo.getLeft(), sourceInfo.getRight(), orderRequest);

        //validate tax
        validateTaxLineRequests(orderRequest, currency);

        // resolve cưstomer
        var customerContextRequest = resolveCustomer(storeId, orderRequest);
        var customerInfo = new CustomerInfo(
                StringUtils.firstNonBlank(
                        orderRequest.getEmail(),
                        customerContextRequest.getEmail()
                ),
                StringUtils.firstNonBlank(
                        orderRequest.getPhone(),
                        customerContextRequest.getPhone()
                ),
                customerContextRequest.getCustomerId(),
                customerContextRequest.isAcceptsMarketing()
        );

        var customAttributes = buildCustomAttributes(orderRequest.getNoteAttributes());

        var addresses = buildAddress(orderRequest.getBillingAddress(), orderRequest.getShippingAddress());

        var combinationLines = buildCombinationLines(orderRequest);

        var lineItems = buildOrderLineItems(storeId, orderRequest.getLineItems(), combinationLines, currency);

        var shippingLines = buildShippingLines(orderRequest.getShippingLines());

        var discountCodes = buildOrderDiscountCodes(
                orderRequest.getDiscountCodes(),
                orderRequest.getTotalDiscounts(),
                lineItems,
                shippingLines,
                currency
        );

        boolean isFromTrustedSource = orderRequest.isFromDraftOrder();

        // allocation discounts
        var allocationDiscountInfo = this.allocateDiscounts(
                isFromTrustedSource,
                discountCodes,
                lineItems,
                shippingLines,
                orderRequest,
                currency
        );

        this.handleTaxLineOrderRequest(orderRequest, lineItems, currency, allocationDiscountInfo.getValue());

        var taxExempt = shouldIgnoreTax(orderRequest);

        var location = validateLocation(storeId, orderRequest.getLocationId());

        var processedAt = Instant.now();

        var order = new Order(
                storeId,
                processedAt,
                customerInfo,
                trackingInfo,
                currency,
                orderRequest.getGateway(),
                orderRequest.getProcessingMethod(),
                orderRequest.getTotalWeight(),
                orderRequest.getNote(),
                orderRequest.getTags(),
                customAttributes,
                addresses.getLeft(),
                addresses.getRight(),
                lineItems,
                shippingLines,
                discountCodes,
                allocationDiscountInfo.getLeft(),
                allocationDiscountInfo.getRight(),
                this.orderIdGenerator,
                taxExempt,
                orderRequest.isTaxesIncluded(),
                orderRequest.getUserId(),
                orderRequest.getLocationId(),
                combinationLines
        );

        // handle fulfill
        if (CollectionUtils.isNotEmpty(orderRequest.getFulfillments())) {
            order.markAsFulfilled();
        }

        // build order routing
        var orderRoutingResponse = processOrderRouting(order, location);
        if (order.getLocationId() == null) {
            orderRoutingResponse.getResults().stream()
                    .map(OrderRoutingResponse.OrderRoutingResult::getLocation)
                    .map(OrderRoutingResponse.OrderRoutingLocation::getId)
                    .map(Long::intValue)
                    .findFirst()
                    .ifPresent(order::updateAssignedLocation);
        }

        var paymentResult = preparePayment(order, orderRequest);
        if (CollectionUtils.isNotEmpty(paymentResult.getTransactions())) {
            paymentResult.getTransactions()
                    .forEach(order::recognizeTransaction);
        }

        this.orderRepository.save(order);

        var orderCreated = OrderCreatedAppEvent.builder()
                .storeId(storeId)
                .orderId(order.getId())
                .orderRoutingResponse(orderRoutingResponse)
                .fulfillmentRequests(orderRequest.getFulfillments())
                .paymentResult(paymentResult)
                .build();

        applicationEventPublisher.publishEvent(orderCreated);

        return order.getId();
    }

    private OrderPaymentResult preparePayment(Order order, OrderCreateRequest request) {
        List<Order.TransactionInput> transactionsInput = new ArrayList<>();
        var checkoutToken = request.getCheckoutToken();
        var transactionsRequest = request.getTransactions();
        var storeId = order.getId().getStoreId();
        var isFromCheckout = false;
        List<Integer> paymentIds = new ArrayList<>();

        /**
         * Checkout: Thanh toán tại quầy
         * Kiểm tra checkoutToken
         * */
        if (StringUtils.isNotBlank(checkoutToken)) {
            var payments = this.paymentRepository.findAvailableByCheckoutToken(storeId, checkoutToken);
            if (CollectionUtils.isNotEmpty(payments)) {
                paymentIds = payments.stream().map(p -> p.getId().getId()).toList();
                var checkoutTransactions = this.transactionRepository.findByPaymentIds(storeId, paymentIds);
                transactionsInput = checkoutTransactions.stream()
                        .map(checkoutOrderTransaction ->
                                Order.TransactionInput.builder()
                                        .id(checkoutOrderTransaction.getId().getId())
                                        .kind(checkoutOrderTransaction.getKind())
                                        .status(checkoutOrderTransaction.getStatus())
                                        .amount(checkoutOrderTransaction.getAmount())
                                        .authorization(checkoutOrderTransaction.getAuthorization())
                                        .errorCode(checkoutOrderTransaction.getErrorCode())
                                        .gateway(checkoutOrderTransaction.getGateway())
                                        .build())
                        .sorted(Comparator.comparing(Order.TransactionInput::getId))
                        .toList();

                isFromCheckout = !transactionsInput.isEmpty();
            }
        }

        // Kiểm tra transactions trong request
        if (CollectionUtils.isNotEmpty(transactionsRequest)
                && CollectionUtils.isEmpty(transactionsInput)) {

            preCheckTransactionsInput(transactionsRequest);

            transactionsInput = transactionsRequest.stream()
                    .map(txnRequest -> {
                        var requestKind = txnRequest.getKind();
                        var parentTransaction = determineParentTransactionInRequest(txnRequest, transactionsRequest);

                        var kind = OrderTransaction.Kind.capture.equals(requestKind)
                                && Objects.isNull(parentTransaction)
                                ? OrderTransaction.Kind.sale : requestKind;

                        var status = OrderTransaction.Kind.capture.equals(requestKind)
                                && Objects.isNull(parentTransaction)
                                ? OrderTransaction.Status.success : txnRequest.getStatus();

                        return Order.TransactionInput.builder()
                                .kind(kind)
                                .status(status)
                                .amount(txnRequest.getAmount())
                                .authorization(txnRequest.getAuthorization())
                                .errorCode(txnRequest.getErrorCode())
                                .gateway(txnRequest.getGateway())
                                .build();
                    })
                    .toList();
        }

        return OrderPaymentResult.builder()
                .isFromCheckout(isFromCheckout)
                .checkoutToken(checkoutToken)
                .paymentIds(paymentIds)
                .transactions(transactionsInput)
                .build();
    }

    private OrderTransactionCreateRequest determineParentTransactionInRequest(OrderTransactionCreateRequest createRequest, List<OrderTransactionCreateRequest> transactionsRequest) {
        int indexFirstAuthorization = -1;
        int indexFirstSalePending = -1;
        int indexFirstSale = -1;
        int indexFirstCaptureSuccess = -1;

        for (int i = 0; i < transactionsRequest.size(); i++) {
            var transactionCreateRequest = transactionsRequest.get(i);

            if (indexFirstAuthorization == -1
                    && transactionCreateRequest.getKind() == OrderTransaction.Kind.authorization) {
                indexFirstAuthorization = i;
            }
            if (indexFirstSalePending == -1
                    && OrderTransaction.Kind.sale.equals(transactionCreateRequest.getKind())
                    && OrderTransaction.Status.pending.equals(transactionCreateRequest.getStatus())) {
                indexFirstSalePending = i;
            }
            if (indexFirstSale == -1
                    && OrderTransaction.Kind.sale.equals(transactionCreateRequest.getKind())) {
                indexFirstSale = i;
            }
            if (indexFirstCaptureSuccess == -1
                    && OrderTransaction.Kind.capture.equals(transactionCreateRequest.getKind())
                    && OrderTransaction.Status.success.equals(transactionCreateRequest.getStatus())) {
                indexFirstCaptureSuccess = i;
            }
        }

        return switch (createRequest.getKind()) {
            case sale -> null;
            case authorization -> null;
            case capture -> null;

            case refund -> null;
            case _void -> null;
        };
    }

    /**
     * Chỉ cho phép truyền amount = null trong trường hợp kind = capture/refund/void: tại vì dựa vào orderTransaction khác để tính
     */
    private void preCheckTransactionsInput(List<OrderTransactionCreateRequest> transactionsRequest) {
        for (OrderTransactionCreateRequest transaction : transactionsRequest) {
            if (transaction.getAmount() == null) {
                var resolvedErrorMessage = this.messageSource.getMessage(
                        "transaction.error.create.amount.required",
                        new Object[]{},
                        LocaleContextHolder.getLocale()
                );
                throw new ConstrainViolationException(
                        "transactions",
                        resolvedErrorMessage
                );
            }
        }
    }


    private OrderRoutingResponse processOrderRouting(Order order, Location location) {
        if (order.getLocationId() == null) {
            return routingSpecificationLocation(order, location);
        }
        var orderRoutingResponse = processOrderRouting(order);
        if (CollectionUtils.isEmpty(orderRoutingResponse.getResults())) {
            throw new ConstrainViolationException("order_routing", "No suitable location");
        }
        return orderRoutingResponse;
    }

    private OrderRoutingResponse processOrderRouting(Order order) {
        int storeId = order.getId().getStoreId();
        var orderRoutingItemsRequest = order.getLineItems().stream()
                .map(this::buildRoutingItemRequst)
                .toList();

        OrderRoutingRequest orderRoutingRequest = new OrderRoutingRequest();
        orderRoutingRequest.setItems(orderRoutingItemsRequest);

        var orderRoutingResultResponses = this.adminClient.orderRouting(orderRoutingRequest, storeId);

        var locationIds = orderRoutingResultResponses.stream().map(OrderRouting::getLocationId).toList();
        List<Location> routingLocations = new ArrayList<>(locationIds.size());
        Lists.partition(locationIds, 50).forEach(partitionedLocationIds -> {
            var locations = new ArrayList<Location>(); // fetch locations
            routingLocations.addAll(locations);
        });

        List<OrderRoutingResponse.OrderRoutingResult> orderRoutingResults = new ArrayList<>();
        for (var orderRoutingResponse : orderRoutingResultResponses) {
            var locationId = orderRoutingResponse.getLocationId();
            var location = routingLocations.stream()
                    .filter(l -> Objects.equals(l.getId(), locationId))
                    .findFirst()
                    .orElseThrow();
            var orderRoutingLocation = OrderRoutingResponse.OrderRoutingLocation.builder()
                    .id(location.getId())
                    .address1(location.getAddress1())
                    .address2(location.getAddress2())
                    .provinceCode(location.getProvinceCode())
                    .districtCode(location.getDistrictCode())
                    .district(location.getDistrict())
                    .ward(location.getWard())
                    .wardCode(location.getWardCode())
                    .country(location.getCountry())
                    .countryCode(location.getCountryCode())
                    .name(location.getName())
                    .phone(location.getPhone())
                    .email(location.getEmail())
                    .zipCode(location.getZip())
                    .build();

            List<OrderRoutingResponse.IndexesItem> itemIndexes = new ArrayList<>();
            for (var item : orderRoutingResponse.getItems()) {
                var orderLineItem = order.getLineItems().get(item.getIndex());

                Integer variantId = orderLineItem.getVariantInfo().getVariantId();
                var indexItem = OrderRoutingResponse.IndexesItem.builder()
                        .variantId(variantId)
                        .inventoryItemId(item.getInventoryItemId())
                        .index(item.getIndex())
                        .build();
                itemIndexes.add(indexItem);
            }

            OrderRoutingResponse.OrderRoutingResult orderRoutingResult = OrderRoutingResponse.OrderRoutingResult.builder()
                    .location(orderRoutingLocation)
                    .indexesItems(itemIndexes)
                    .build();
            orderRoutingResults.add(orderRoutingResult);
        }

        return new OrderRoutingResponse(orderRoutingResults);
    }

    private OrderRoutingRequest.OrderRoutingItemRequest buildRoutingItemRequst(LineItem item) {
        var orderRoutingItemRequest = new OrderRoutingRequest.OrderRoutingItemRequest();
        orderRoutingItemRequest.setVariantId(item.getVariantInfo().getVariantId());
        orderRoutingItemRequest.setQuantity(BigDecimal.valueOf(item.getQuantity()));
        orderRoutingItemRequest.setRequireShipping(item.getVariantInfo().isRequireShipping());
        return orderRoutingItemRequest;
    }

    private OrderRoutingResponse routingSpecificationLocation(Order order, Location location) {
        OrderRoutingResponse.OrderRoutingLocation orderRoutingLocation = OrderRoutingResponse.OrderRoutingLocation.builder()
                .id(location.getId())
                .address1(location.getAddress1())
                .provinceCode(location.getProvinceCode())
                .province(location.getProvince())
                .districtCode(location.getDistrictCode())
                .district(location.getDistrict())
                .ward(location.getWard())
                .wardCode(location.getWardCode())
                .country(location.getCountry())
                .countryCode(location.getCountryCode())
                .name(location.getName())
                .phone(location.getPhone())
                .email(location.getEmail())
                .zipCode(location.getZip())
                .build();

        List<OrderRoutingResponse.IndexesItem> items = new ArrayList<>();
        for (int i = 0; i < order.getLineItems().size(); i++) {
            var orderLineItem = order.getLineItems().get(i);
            Integer variantId = orderLineItem.getVariantInfo().getVariantId();
            Integer inventoryItemId = orderLineItem.getVariantInfo().getInventoryItemId();
            OrderRoutingResponse.IndexesItem item = OrderRoutingResponse.IndexesItem.builder()
                    .index(i)
                    .variantId(variantId)
                    .inventoryItemId(inventoryItemId)
                    .build();
            items.add(item);
        }

        var routingResult = OrderRoutingResponse.OrderRoutingResult.builder()
                .location(orderRoutingLocation)
                .indexesItems(items)
                .build();
        return OrderRoutingResponse.builder()
                .results(List.of(routingResult))
                .build();
    }

    private Location validateLocation(int storeId, Integer locationId) {
        if (locationId == null) return new Location(); //TODO: return default location of store
        return new Location();
    }

    private boolean shouldIgnoreTax(OrderCreateRequest orderRequest) {
        return orderRequest.isTaxExempt();
    }

    private void handleTaxLineOrderRequest(
            OrderCreateRequest orderRequest,
            List<LineItem> lineItems,
            Currency currency,
            List<DiscountAllocation> discountAllocations
    ) {
        if (CollectionUtils.isEmpty(orderRequest.getTaxLines())) return;

        var lineItemDiscounts = discountAllocations.stream()
                .filter(discount -> discount.getTargetType() == DiscountAllocation.TargetType.line_item)
                .collect(Collectors.groupingBy(DiscountAllocation::getTargetId));

        final Map<Integer, BigDecimal> discountLineMap = new HashMap<>();
        final List<LineItem> taxableLineItems = new ArrayList<>();
        final List<LineItem> feeTaxLineItems = new ArrayList<>();

        for (var lineItem : lineItems) {
            var totalDiscount = lineItemDiscounts.getOrDefault(lineItem.getId(), List.of())
                    .stream()
                    .map(DiscountAllocation::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            var originalPrice = lineItem.getOriginalTotal();
            discountLineMap.put(lineItem.getId(), totalDiscount);
            if (originalPrice.compareTo(totalDiscount) > 0) {
                taxableLineItems.add(lineItem);
            } else {
                feeTaxLineItems.add(lineItem);
            }
        }

        var totalPrice = lineItems.stream()
                .map(line ->
                        line.getOriginalTotal()
                                .subtract(discountLineMap.get(line.getId())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        //Phải phân bổ thuế về tất cả các line
        var taxLineIds = this.orderIdGenerator.generateTaxLineIds(orderRequest.getTaxLines().size() * lineItems.size());
        for (int i = 0; i < taxableLineItems.size(); i++) {
            var lineItem = taxableLineItems.get(i);
            var taxLinesForLine = buildTaxLineForLine(
                    taxLineIds,
                    orderRequest.getTaxLines(),
                    lineItem,
                    discountLineMap.get(lineItem.getId()),
                    currency,
                    i == taxableLineItems.size() - 1,
                    totalPrice
            );
            lineItem.changeTax(taxLinesForLine);
        }

        for (var lineItem : feeTaxLineItems) {
            List<TaxLine> taxLines = new ArrayList<>();
            for (var taxLineRequest : orderRequest.getTaxLines()) {
                var taxLine = new TaxLine(
                        taxLineIds.removeFirst(),
                        taxLineRequest.getTitle(),
                        BigDecimal.ZERO,
                        taxLineRequest.getRate(),
                        lineItem.getId(),
                        TaxLine.TargetType.line_item,
                        lineItem.getQuantity()
                );
                taxLines.add(taxLine);
            }
            lineItem.changeTax(taxLines);
        }
    }

    private List<TaxLine> buildTaxLineForLine(
            Deque<Integer> taxLineIds,
            List<OrderCreateRequest.TaxLineRequest> taxLineRequests,
            LineItem lineItem,
            BigDecimal lineItemDiscountPrice,
            Currency currency,
            boolean isLastLine,
            BigDecimal totalPrice
    ) {
        List<TaxLine> taxLines = new ArrayList<>();
        if (isLastLine) {
            for (var taxRequest : taxLineRequests) {
                var taxLine = new TaxLine(
                        taxLineIds.removeFirst(),
                        taxRequest.getTitle(),
                        taxRequest.getPrice(),
                        taxRequest.getRate(),
                        lineItem.getId(),
                        TaxLine.TargetType.line_item,
                        lineItem.getQuantity()
                );
                taxLines.add(taxLine);
            }
        } else {
            var remainingLinePrice = lineItem.getOriginalTotal().subtract(lineItemDiscountPrice);
            for (var taxLineRequest : taxLineRequests) {
                var taxPrice = taxLineRequest.getPrice()
                        .multiply(remainingLinePrice)
                        .divide(totalPrice, currency.getDefaultFractionDigits(), RoundingMode.UP);
                var taxLine = new TaxLine(
                        taxLineIds.removeFirst(),
                        taxLineRequest.getTitle(),
                        taxPrice,
                        taxLineRequest.getRate(),
                        lineItem.getId(),
                        TaxLine.TargetType.line_item,
                        lineItem.getQuantity()
                );
                var remainingTaxPrice = taxLineRequest.getPrice().subtract(taxPrice);
                taxLineRequest.setPrice(remainingTaxPrice);
                taxLines.add(taxLine);
            }
        }
        return taxLines;
    }

    private Pair<List<DiscountApplication>, List<DiscountAllocation>> allocateDiscounts(
            boolean isFromTrustedSource,
            List<OrderDiscountCode> discountCodes,
            List<LineItem> lineItems,
            List<ShippingLine> shippingLines,
            OrderCreateRequest orderRequest,
            Currency currency
    ) {
        var applicationRequests = CollectionUtils.isNotEmpty(orderRequest.getDiscountApplications())
                ? orderRequest.getDiscountApplications()
                : new ArrayList<OrderCreateRequest.DiscountApplicationRequest>();
        var allocationRequests = new ArrayList<OrderCreateRequest.DiscountAllocationRequest>();

        for (int i = 0; i < lineItems.size(); i++) {
            var lineItem = lineItems.get(i);
            var lineItemRequest = orderRequest.getLineItems().get(i);
            // Nếu như nguồn xác định thì sẽ tính theo allocations
            if (isFromTrustedSource) {
                if (CollectionUtils.isEmpty(lineItemRequest.getDiscountAllocations())) {
                    continue;
                }
                for (var allocationRequest : lineItemRequest.getDiscountAllocations()) {
                    allocationRequest.setTargetType(DiscountAllocation.TargetType.line_item);
                    allocationRequest.setTargetId(lineItem.getId());
                    allocationRequests.add(allocationRequest);
                }
            } else { // còn lại sẽ tính theo totalDiscount
                if (!NumberUtils.isPositive(lineItemRequest.getTotalDiscount())) {
                    continue;
                }
                var applicationIndex = applicationRequests.size();
                var applicationRequest = OrderCreateRequest.DiscountApplicationRequest.builder()
                        .code("manual")
                        .title("manual")
                        .description("discount description")
                        .targetType(DiscountApplication.TargetType.line_item)
                        .valueType(DiscountApplication.ValueType.fixed_amount)
                        .value(lineItemRequest.getTotalDiscount())
                        .ruleType(DiscountApplication.RuleType.product)
                        .build();
                //NOTE: amount apply for all line_item quantity
                var allocationRequest = OrderCreateRequest.DiscountAllocationRequest.builder()
                        .amount(lineItemRequest.getTotalDiscount())
                        .discountedAmount(lineItemRequest.getTotalDiscount())
                        .targetType(DiscountAllocation.TargetType.line_item)
                        .targetId(lineItem.getId())
                        .discountApplicationIndex(applicationIndex)
                        .build();
                applicationRequests.add(applicationRequest);
                allocationRequests.add(allocationRequest);
            }
        }
        if (CollectionUtils.isNotEmpty(shippingLines)) {
            for (int i = 0; i < shippingLines.size(); i++) {
                var shippingLine = shippingLines.get(i);
                var shippingLineRequest = orderRequest.getShippingLines().get(i);
                if (CollectionUtils.isEmpty(shippingLineRequest.getDiscountAllocations()))
                    continue;
                for (var allocationRequest : shippingLineRequest.getDiscountAllocations()) {
                    allocationRequest.setTargetId(shippingLine.getId());
                    allocationRequest.setTargetType(DiscountAllocation.TargetType.shipping);
                    allocationRequests.add(allocationRequest);
                }
            }
        }

        if (CollectionUtils.isNotEmpty(discountCodes)) {
            var discountCode = discountCodes.get(0);
            switch (discountCode.getType()) {
                case shipping, shipping_line -> {
                    if (CollectionUtils.isNotEmpty(shippingLines)) {
                        var applicationRequest = OrderCreateRequest.DiscountApplicationRequest.builder()
                                .code(discountCode.getCode())
                                .title("shipping_title_manual")
                                .description("discount_code_shipping_description")
                                .targetType(DiscountApplication.TargetType.shipping_line)
                                .valueType(DiscountApplication.ValueType.fixed_amount)
                                .ruleType(DiscountApplication.RuleType.order)
                                .value(discountCode.getValue())
                                .index(allocationRequests.size())
                                .build();

                        var allocationRequest = OrderCreateRequest.DiscountAllocationRequest.builder()
                                .targetType(DiscountAllocation.TargetType.shipping)
                                .discountApplicationIndex(applicationRequest.getIndex())
                                .build();

                        var shippingLineWithId = shippingLines.stream()
                                .collect(Collectors.toMap(
                                        ShippingLine::getId,
                                        Function.identity(),
                                        (first, second) -> second,
                                        LinkedHashMap::new
                                ));
                        this.allocateAmount(
                                shippingLineWithId,
                                ShippingLine::getPrice,
                                discountCode.getAmount(),
                                currency,
                                applicationRequest,
                                allocationRequest,
                                applicationRequests,
                                allocationRequests);
                    }
                }
                case percentage, fixed_amount -> {
                    var applicationRequest = OrderCreateRequest.DiscountApplicationRequest.builder()
                            .code(discountCode.getCode())
                            .title("line_items_discount_title")
                            .description("")
                            .value(discountCode.getValue())
                            .targetType(DiscountApplication.TargetType.line_item)
                            .valueType(
                                    discountCode.getType() == OrderDiscountCode.ValueType.percentage
                                            ? DiscountApplication.ValueType.percentage
                                            : DiscountApplication.ValueType.fixed_amount)
                            .ruleType(DiscountApplication.RuleType.order)
                            .index(applicationRequests.size())
                            .build();

                    var allocationRequest = OrderCreateRequest.DiscountAllocationRequest.builder()
                            .targetType(DiscountAllocation.TargetType.line_item)
                            .discountApplicationIndex(applicationRequest.getIndex())
                            .build();

                    var lineItemWithId = lineItems.stream()
                            .filter(line -> line.getDiscountedTotal().compareTo(BigDecimal.ZERO) != 0)
                            .collect(Collectors.toMap(
                                    LineItem::getId,
                                    Function.identity(),
                                    (first, second) -> second,
                                    LinkedHashMap::new
                            ));
                    this.allocateAmount(
                            lineItemWithId,
                            LineItem::getDiscountedTotal,
                            discountCode.getAmount(),
                            currency,
                            applicationRequest,
                            allocationRequest,
                            applicationRequests,
                            allocationRequests);
                }
            }
        }

        if (CollectionUtils.isEmpty(applicationRequests) || CollectionUtils.isEmpty(allocationRequests)) {
            return Pair.of(List.of(), List.of());
        }

        var applicationIds = this.orderIdGenerator.generateDiscountApplicationIds(applicationRequests.size());
        var applications = applicationRequests.stream()
                .map(request ->
                        new DiscountApplication(
                                applicationIds.removeFirst(),
                                request.getValue(),
                                request.getValueType(),
                                request.getTargetType(),
                                request.getRuleType(),
                                request.getCode(),
                                request.getTitle(),
                                request.getDescription()
                        ))
                .toList();
        var allocationIds = this.orderIdGenerator.generateDiscountAllocationIds(allocationRequests.size());
        var allocations = allocationRequests.stream()
                .map(request ->
                        new DiscountAllocation(
                                allocationIds.removeFirst(),
                                request.getAmount(),
                                request.getTargetId(),
                                request.getTargetType(),
                                applications.get(request.getDiscountApplicationIndex()).getId(),
                                request.getDiscountApplicationIndex()
                        ))
                .toList();
        return Pair.of(applications, allocations);
    }

    private <T> void allocateAmount(
            LinkedHashMap<Integer, T> resourcesWithId,
            Function<T, BigDecimal> priceFunction,
            BigDecimal discountAmount,
            Currency currency,
            OrderCreateRequest.DiscountApplicationRequest applicationRequest,
            OrderCreateRequest.DiscountAllocationRequest allocationRequest,
            List<OrderCreateRequest.DiscountApplicationRequest> applicationRequests,
            List<OrderCreateRequest.DiscountAllocationRequest> allocationRequests
    ) {
        if (resourcesWithId.isEmpty()) return;
        // add applicatonRequest
        applicationRequests.add(applicationRequest);

        var totalResourcePrice = resourcesWithId.values().stream()
                .map(priceFunction)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var resources = resourcesWithId.values().stream().toList();

        var resourceCount = resources.size();
        BigDecimal appliedAmount = BigDecimal.ZERO;
        int index = 0;
        for (var entry : resourcesWithId.entrySet()) {
            BigDecimal allocationAmount;
            var resource = entry.getValue();
            if (index != resourceCount - 1) {
                allocationAmount = priceFunction.apply(resource).multiply(totalResourcePrice)
                        .setScale(currency.getDefaultFractionDigits(), RoundingMode.HALF_UP);
                appliedAmount = appliedAmount.add(allocationAmount);
            } else {
                allocationAmount = discountAmount.subtract(appliedAmount);
            }
            var allocation = allocationRequest.toBuilder()
                    .amount(allocationAmount)
                    .targetId(entry.getKey())
                    .build();
            allocationRequests.add(allocation);
            index++;
        }
    }

    /**
     * Tính giảm gia của đơn hàng
     */
    private List<OrderDiscountCode> buildOrderDiscountCodes(
            List<OrderCreateRequest.DiscountCodeRequest> discountCodeRequests,
            BigDecimal totalDiscounts,
            List<LineItem> lineItems,
            List<ShippingLine> shippingLines,
            Currency currency
    ) {
        var totalLineItemPrice = lineItems.stream().map(LineItem::getDiscountedTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        var totalShippingPrice = shippingLines.stream().map(ShippingLine::getPrice).reduce(BigDecimal.ZERO, BigDecimal::add);

        /**
         * Sẽ có 2 giá trị dựa vào để tinh toán
         * - discountCodeRequests
         * - totalDiscounts
         * */

        /**
         * Nếu discountCodeRequests empty => dựa vào totalDiscounts => chỉ áp dụng cho lineItems
         * - Tính toán amount sau
         * */
        if (CollectionUtils.isEmpty(discountCodeRequests)) {
            if (!NumberUtils.isPositive(totalDiscounts)) {
                return List.of();
            }
            var discountCodeId = this.orderIdGenerator.generateDiscountCodeId();
            var discountValue = totalDiscounts.min(totalLineItemPrice)
                    .setScale(currency.getDefaultFractionDigits(), RoundingMode.UP);
            var discountCode = new OrderDiscountCode(
                    discountCodeId,
                    "Custom discount",
                    discountValue,
                    OrderDiscountCode.ValueType.fixed_amount,
                    true
            );
            return List.of(discountCode);
        }

        //NOTE: nếu như có discountCodeRequest => Thì chỉ xử lý 1 discountCode, tính toán sẽ theo discountCode
        var discountCodeRequest = discountCodeRequests.get(0);
        if (discountCodeRequest.getAmount() == null || StringUtils.isBlank(discountCodeRequest.getCode())) {
            log.warn("Discount code invalid {}", discountCodeRequest);
            return List.of();
        }
        if (discountCodeRequest.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new ConstrainViolationException(
                    "discount_code",
                    "must be greater than or equal to 0"
            );
        }
        if (discountCodeRequest.getType() == null) {
            log.warn("required discount code type");
            discountCodeRequest.setType(OrderDiscountCode.ValueType.fixed_amount);
        }

        var discountCodeId = this.orderIdGenerator.generateDiscountCodeId();
        var discountCode = switch (discountCodeRequest.getType()) {
            case fixed_amount -> {
                var discountValue = discountCodeRequest.getAmount().min(totalLineItemPrice)
                        .setScale(currency.getDefaultFractionDigits(), RoundingMode.UP);
                yield new OrderDiscountCode(
                        discountCodeId,
                        discountCodeRequest.getCode(),
                        discountValue,
                        OrderDiscountCode.ValueType.fixed_amount,
                        true
                );
            }
            case percentage -> {
                var discountValue = discountCodeRequest.getAmount().min(BigDecimals.ONE_HUND0RED);
                var discount = new OrderDiscountCode(
                        discountCodeId,
                        discountCodeRequest.getCode(),
                        discountValue,
                        OrderDiscountCode.ValueType.percentage,
                        true
                );
                var discountAmount = discountValue.multiply(totalLineItemPrice).movePointLeft(2)
                        .setScale(currency.getDefaultFractionDigits(), RoundingMode.UP);
                discount.setAmount(discountAmount);
                yield discount;
            }
            case shipping_line, shipping -> {
                //NOTE: Tính theo fixedValue, áp dụng cho shipping
                var shippingDiscountValue = discountCodeRequest.getAmount().min(totalShippingPrice)
                        .setScale(currency.getDefaultFractionDigits(), RoundingMode.UP);
                yield new OrderDiscountCode(
                        discountCodeId,
                        discountCodeRequest.getCode(),
                        shippingDiscountValue,
                        discountCodeRequest.getType(),
                        true
                );
            }
        };
        var discounts = List.of(discountCode);
        this.reCalculateDiscountCodes(discounts, lineItems, shippingLines, currency);
        return discounts;
    }

    private void reCalculateDiscountCodes(
            List<OrderDiscountCode> discounts,
            List<LineItem> lineItems,
            List<ShippingLine> shippingLines,
            Currency currency
    ) {
        //Recalculate fill amount to discounts
        if (CollectionUtils.isEmpty(discounts)) return;
        var discountsNullAmount = discounts.stream()
                .filter(discount -> discount.getAmount() == null)
                .toList();
        if (CollectionUtils.isNotEmpty(discountsNullAmount)) {
            var totalLineItemPrice = lineItems.stream().map(LineItem::getDiscountedTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            for (var discountCode : discountsNullAmount) {
                switch (discountCode.getType()) {
                    case shipping, shipping_line -> {
                        var totalShippingPrice = CollectionUtils.isEmpty(shippingLines)
                                ? BigDecimal.ZERO
                                : shippingLines.stream().map(ShippingLine::getPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
                        var discountValue = discountCode.getValue();
                        BigDecimal discountAmount = calculateShippingDiscountAmount(discountValue, totalShippingPrice, currency);
                        discountCode.setAmount(discountAmount);
                    }
                    case fixed_amount -> {
                        var discountAmount = discountCode.getValue().min(totalLineItemPrice)
                                .setScale(currency.getDefaultFractionDigits(), RoundingMode.UP);
                        discountCode.setAmount(discountAmount);
                    }
                    case percentage -> {
                        var discountAmount = discountCode.getValue().multiply(totalLineItemPrice)
                                .divide(BigDecimals.ONE_HUND0RED, currency.getDefaultFractionDigits(), RoundingMode.UP);
                        discountCode.setAmount(discountAmount);
                    }
                }
            }
        }
    }

    private BigDecimal calculateShippingDiscountAmount(BigDecimal discountValue, BigDecimal totalShippingPrice, Currency currency) {
        BigDecimal discountAmount;
        if (discountValue.compareTo(BigDecimals.ONE_HUND0RED) <= 0) {
            discountAmount = discountValue.multiply(totalShippingPrice)
                    .divide(BigDecimals.ONE_HUND0RED, currency.getDefaultFractionDigits(), RoundingMode.UP);
        } else {
            discountAmount = discountValue.min(totalShippingPrice)
                    .setScale(currency.getDefaultFractionDigits(), RoundingMode.UP);
        }
        return discountAmount;
    }

    private List<ShippingLine> buildShippingLines(List<OrderCreateRequest.ShippingLineRequest> shippingLineRequests) {
        if (CollectionUtils.isEmpty(shippingLineRequests))
            return List.of();

        var shippingLineIds = this.orderIdGenerator.generateShippingLineIds(shippingLineRequests.size());
        return shippingLineRequests.stream()
                .map((request) ->
                        buildShippingLine(request, shippingLineIds.removeFirst()))
                .toList();
    }

    private ShippingLine buildShippingLine(OrderCreateRequest.ShippingLineRequest request, int id) {
        var shippingLine = new ShippingLine(
                id,
                request.getTitle(),
                request.getCode(),
                request.getSource(),
                request.getPrice()
        );
        if (CollectionUtils.isNotEmpty(request.getTaxLines())) {
            var taxLineIds = this.orderIdGenerator.generateTaxLineIds(request.getTaxLines().size());
            var taxLines = request.getTaxLines().stream()
                    .map(taxLineRequest ->
                            new TaxLine(
                                    taxLineIds.removeFirst(),
                                    taxLineRequest.getTitle(),
                                    taxLineRequest.getPrice(),
                                    taxLineRequest.getRate(),
                                    id,
                                    TaxLine.TargetType.shipping,
                                    0
                            ))
                    .toList();
            shippingLine.setTaxLines(taxLines);
        }
        return shippingLine;
    }

    private List<LineItem> buildOrderLineItems(int storeId,
                                               List<OrderCreateRequest.LineItemRequest> lineItemRequests,
                                               List<CombinationLine> combinationLines,
                                               Currency currency) {
        if (CollectionUtils.isEmpty(lineItemRequests))
            throw new IllegalArgumentException("line_items must not be empty");

        var productInfo = getProductInfo(storeId, lineItemRequests);

        var lineItemIds = this.orderIdGenerator.generateLineItemIds(lineItemRequests.size());
        List<LineItem> lineItems = new ArrayList<>();
        for (int i = 0; i < lineItemRequests.size(); i++) {
            var lineItemRequest = lineItemRequests.get(i);
            VariantDto variant = null;
            ProductDto product = null;
            var variantId = lineItemRequest.getVariantId();
            if (variantId != null) {
                variant = productInfo.variants.get(variantId);
                if (variant != null) {
                    product = productInfo.products.get(variant.getProductId());
                }
            }
            if (variant == null && (lineItemRequest.getPrice() == null || lineItemRequest.getPrice().compareTo(BigDecimal.ZERO) < 0)) {
                throw new ConstrainViolationException(UserError.builder()
                        .fields(List.of("line_items"))
                        .message("line_items[%s] is custom variant. You must fill price for it".formatted(i))
                        .build());
            }
            if (variant == null && StringUtils.isBlank(lineItemRequest.getTitle())) {
                throw new ConstrainViolationException(
                        "line_items",
                        "line_items[%s] is custom line, must have title".formatted(i)
                );
            }

            var combinationIndex = lineItemRequest.getCombinationLineIndex();
            var combinationSize = combinationLines.size();
            Integer combinationLineId = null;
            if (combinationIndex != null && combinationIndex >= 0 && combinationIndex < combinationSize) {
                combinationLineId = combinationLines.get(i).getId();
            }

            var lineItem = buildLineItem(lineItemIds.removeFirst(), variant, product, lineItemRequest, combinationLineId, currency);
            lineItems.add(lineItem);
        }

        return lineItems;
    }

    private LineItem buildLineItem(
            Integer id,
            VariantDto variant,
            ProductDto product,
            OrderCreateRequest.LineItemRequest request,
            Integer combinationLineId,
            Currency currency
    ) {
        boolean productExists = false;
        Integer variantId = null;
        Integer productId = null;
        String title = request.getTitle();
        String variantTitle = request.getVariantTitle();
        String vendor = request.getVendor();
        String sku = request.getSku();
        String discountCode = null;
        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal price = NumberUtils.isPositive(request.getPrice())
                ? request.getPrice().setScale(currency.getDefaultFractionDigits(), RoundingMode.HALF_UP)
                : request.getPrice();
        int weight = request.getGrams();
        String inventoryManagement = null;
        Boolean requireShipping = request.getRequireShipping();
        List<TaxLine> taxLines = buildTaxLines(id, request.getQuantity(), request.getTaxLines());
        Boolean taxable = request.getTaxable();
        Integer inventoryItemId = null;
        String unit = request.getUnit();

        if (variant != null && product != null) {
            productExists = true;
            variantId = variant.getId();
            productId = product.getId();
            inventoryManagement = variant.getInventoryManagement();

            if (requireShipping == null) requireShipping = variant.isRequiresShipping();
            inventoryItemId = variant.getInventoryItemId();

            if (StringUtils.isBlank(title)) title = product.getName();
            if (StringUtils.isBlank(variantTitle)) variantTitle = variant.getTitle();
            if (StringUtils.isBlank(vendor)) vendor = product.getVendor();
            if (StringUtils.isBlank(sku)) sku = variant.getSku();

            if (price == null) {
                price = variant.getPrice()
                        .setScale(currency.getDefaultFractionDigits(), RoundingMode.HALF_UP);
            }
            if (weight == 0) weight = variant.getGrams();
            if (taxable == null) taxable = variant.isTaxable();
        }
        if (requireShipping == null) requireShipping = true;
        if (price != null && request.getTotalDiscount() != null) {
            discountCode = request.getDiscountCode();
            discount = request.getTotalDiscount().min(price);
        }
        taxable = taxable != null && taxable;

        var componentVariantId = variantId != null ? variantId : request.getVariantId();
        var componentLineKey = NumberUtils.isPositive(combinationLineId) && NumberUtils.isPositive(componentVariantId)
                ? String.format("%s-%s", combinationLineId, componentVariantId)
                : null;

        List<OrderCustomAttribute> attributes = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(request.getProperties())) {
            var properties = request.getProperties().stream()
                    .filter(property -> StringUtils.isNotBlank(property.getName()))
                    .filter(property -> property.getValue() != null)
                    .collect(Collectors.toMap(
                            CustomAttributeRequest::getName,
                            CustomAttributeRequest::getName,
                            (first, second) -> second,
                            LinkedHashMap::new
                    ));
            if (!properties.isEmpty()) {
                attributes = properties.entrySet().stream()
                        .map(entry ->
                                new OrderCustomAttribute(
                                        entry.getKey(),
                                        entry.getValue()
                                ))
                        .toList();
            }
        }

        var variantInfo = VariantInfo.builder()
                .productId(productId)
                .variantId(variantId)
                .title(title)
                .variantTitle(variantTitle)
                .productExists(productExists)
                .vendor(vendor)
                .sku(sku)
                .grams(weight)
                .requireShipping(requireShipping)
                .inventoryManagement(inventoryManagement)
                .restockable(inventoryManagement != null)
                .inventoryItemId(inventoryItemId)
                .unit(unit)
                .build();

        return new LineItem(
                id,
                request.getQuantity(),
                price,
                variantInfo,
                attributes,
                taxLines,
                taxable,
                discountCode,
                discount,
                componentLineKey
        );
    }

    private List<TaxLine> buildTaxLines(Integer lineItemId, int quantity, List<OrderCreateRequest.TaxLineRequest> taxLineRequests) {
        List<TaxLine> taxLines = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(taxLineRequests)) {
            var ids = this.orderIdGenerator.generateTaxLineIds(taxLineRequests.size());
            taxLines = taxLineRequests.stream()
                    .map(taxLineRequest ->
                            new TaxLine(
                                    ids.removeFirst(),
                                    taxLineRequest.getTitle(),
                                    taxLineRequest.getPrice(),
                                    taxLineRequest.getRate(),
                                    lineItemId,
                                    TaxLine.TargetType.line_item,
                                    quantity
                            ))
                    .toList();
        }
        return taxLines;
    }

    private ProductInfo getProductInfo(int storeId, List<OrderCreateRequest.LineItemRequest> lineItemRequests) {
        var variantIds = lineItemRequests.stream()
                .map(OrderCreateRequest.LineItemRequest::getVariantId)
                .filter(NumberUtils::isPositive)
                .distinct()
                .toList();
        if (CollectionUtils.isEmpty(variantIds))
            return new ProductInfo(Map.of(), Map.of());

        var variants = this.productDao.findVariantByListId(storeId, variantIds).stream()
                .collect(Collectors.toMap(VariantDto::getId, Function.identity()));
        var variantNotFoundString = variantIds.stream()
                .filter(id -> !variants.containsKey(id))
                .map(String::valueOf)
                .collect(Collectors.joining(", "));
        if (StringUtils.isNotBlank(variantNotFoundString)) {
            throw new ConstrainViolationException(UserError.builder()
                    .code("not found")
                    .message(variantNotFoundString)
                    .fields(List.of("variant_id"))
                    .build());
        }

        var productIds = variants.values().stream()
                .map(VariantDto::getProductId)
                .distinct().toList();
        var products = this.productDao.findProductByListId(storeId, productIds).stream()
                .collect(Collectors.toMap(ProductDto::getId, Function.identity()));

        return new ProductInfo(variants, products);
    }

    @Transactional
    public int createRefund(OrderId orderId, RefundRequest refundRequest) {
        var store = this.findStoreById(orderId.getStoreId());
        var order = this.findOrderById(orderId);

        var refundResult = addRefund(order, refundRequest);

        this.orderRepository.save(order);

        var refundCreatedEvent = buildRefundCreatedEvent(
                order,
                refundResult.refund(),
                refundRequest.getTransactions()
        );
        this.applicationEventPublisher.publishEvent(refundCreatedEvent);

        return refundResult.refund().getId();
    }

    private RefundCreatedAppEvent buildRefundCreatedEvent(
            Order order,
            Refund refund,
            List<OrderTransactionCreateRequest> transactions
    ) {
        List<RefundCreatedAppEvent.RestockLineItem> restockLineItems = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(refund.getRefundLineItems())) {
            restockLineItems = refund.getRefundLineItems().stream()
                    .map(refundItem ->
                            new RefundCreatedAppEvent.RestockLineItem(
                                    refundItem.getLocationId() == null ? 0 : refundItem.getLocationId(),
                                    refundItem.getLineItemId(),
                                    refundItem.getQuantity(),
                                    refundItem.isRemoval(),
                                    refundItem.getType() != RefundLineItem.RestockType.no_restock
                            ))
                    .toList();
        }

        return new RefundCreatedAppEvent(
                order,
                restockLineItems,
                transactions
        );
    }

    private RefundResult addRefund(Order order, RefundRequest refundRequest) {
        var moneyInfo = order.getMoneyInfo();
        if (ObjectUtils.anyNull(moneyInfo.getTotalReceived(), moneyInfo.getNetPayment())) {
            log.warn("Order {} has no total received and net payment", order.getId());
            var transactions = this.transactionRepository.findByOrderId(order.getId());
            order.recalculatePaymentState(transactions);
        }

        var refundResult = buildRefund(order, refundRequest);

        if (refundResult.refund().isEmpty() && CollectionUtils.isEmpty(refundResult.transactions())) {
            throw new ConstrainViolationException(UserError.builder()
                    .code("required")
                    .fields(List.of("refund_line_items", "order_adjustments", "transactions"))
                    .message("Refund must contains at least one refund_line_items")
                    .build());
        }

        order.addRefund(refundResult.refund());

        var refundTaxLineInfo = recreateRefundTaxLines(refundResult.refund(), order);

        order.updateOrInsertRefundTaxLine(refundTaxLineInfo.getKey(), refundTaxLineInfo.getValue());

        return refundResult;
    }

    private Pair<Map<Integer, BigDecimal>, List<RefundTaxLine>> recreateRefundTaxLines(Refund refund, Order order) {
        Map<Integer, BigDecimal> refundingLineItemTaxes = new LinkedHashMap<>();
        Map<Integer, BigDecimal> refundingShippingLineTaxes = new LinkedHashMap<>();

        if (CollectionUtils.isNotEmpty(refund.getRefundLineItems())) {
            var refundingLineTaxes = new RefundingLineItemTaxes(
                    order.getLineItems(),
                    order.getRefundTaxLines(),
                    refund.getRefundLineItems()
            );
            refundingLineItemTaxes = refundingLineTaxes.getRefundingTaxes();
        }

        if (CollectionUtils.isNotEmpty(refund.getOrderAdjustments())) {
            var refundShippingLines = refund.getOrderAdjustments().stream()
                    .filter(adjustment -> adjustment.getKind() == OrderAdjustment.RefundKind.shipping_refund)
                    .collect(Collectors.toSet());
            if (!refundShippingLines.isEmpty()) {
                var refundShippingTaxes = new RefundingShippingTaxes(
                        order.getShippingLines(),
                        order.getRefundTaxLines(),
                        refundShippingLines
                );
                refundingShippingLineTaxes = refundShippingTaxes.getRefundingTaxes();
            }
        }

        if (refundingLineItemTaxes.isEmpty() && refundingShippingLineTaxes.isEmpty()) {
            return Pair.of(Map.of(), List.of());
        }

        var allTaxLineIds = Stream.concat(
                        refundingLineItemTaxes.keySet().stream(),
                        refundingShippingLineTaxes.keySet().stream()
                )
                .distinct()
                .toList();
        var refundedTaxLineIds = CollectionUtils.isEmpty(order.getRefundTaxLines())
                ? List.of()
                : order.getRefundTaxLines().stream().map(RefundTaxLine::getTaxLineId).toList();

        var needGenerateIds = allTaxLineIds.stream()
                .filter(id -> refundedTaxLineIds.stream().noneMatch(tlId -> tlId == id))
                .toList();

        var newRefundTaxLineIds = this.orderIdGenerator.generateRefundTaxLineIds(needGenerateIds.size());

        return this.getRefundTaxLineInfo(newRefundTaxLineIds, refundingLineItemTaxes, refundingShippingLineTaxes, order.getRefundTaxLines());
    }

    private Pair<Map<Integer, BigDecimal>, List<RefundTaxLine>> getRefundTaxLineInfo(
            Deque<Integer> newRefundTaxLineIds,
            Map<Integer, BigDecimal> refundingLineItemTaxes,
            Map<Integer, BigDecimal> refundingShippingLineTaxes,
            List<RefundTaxLine> refundedTaxLines
    ) {
        Map<Integer, BigDecimal> updateRefundedTaxLine = new LinkedHashMap<>();

        var refunding = Stream.concat(
                        refundingLineItemTaxes.entrySet().stream(),
                        refundingShippingLineTaxes.entrySet().stream()
                )
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                Map.Entry::getValue,
                                BigDecimal::add
                        )
                ));

        if (CollectionUtils.isNotEmpty(refundedTaxLines)) {
            refundedTaxLines
                    .forEach(refunded -> {
                        var refundTax = refunding.get(refunded.getTaxLineId());
                        if (refundTax != null) {
                            updateRefundedTaxLine.put(refunded.getTaxLineId(), refundTax);
                        }
                    });

        }

        List<RefundTaxLine> refundTaxLines = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(newRefundTaxLineIds)) {
            refundTaxLines = buildNewRefundTaxLines(newRefundTaxLineIds, refunding, updateRefundedTaxLine);
        }

        return Pair.of(updateRefundedTaxLine, refundTaxLines);
    }

    private List<RefundTaxLine> buildNewRefundTaxLines(
            Deque<Integer> refundTaxLineIds,
            Map<Integer, BigDecimal> refunding,
            Map<Integer, BigDecimal> updateRefundedTaxLine
    ) {
        return refunding.entrySet()
                .stream()
                .filter(entry -> !updateRefundedTaxLine.containsKey(entry.getKey()))
                .map(entry ->
                        new RefundTaxLine(
                                refundTaxLineIds.removeFirst(),
                                entry.getKey(),
                                entry.getValue()
                        ))
                .toList();
    }

    public void editLineItems(OrderId orderId, OrderEdit orderEdit, OrderEditRequest.Commit request) {
        this.orderEditCommitService.editLineItems(orderId, orderEdit, request);
    }

    final static class RefundingShippingTaxes extends RefundingTaxAbstract<ShippingLine, OrderAdjustment> {

        RefundingShippingTaxes(List<ShippingLine> appliedTax, List<RefundTaxLine> refundTaxLines, Set<OrderAdjustment> refunded) {
            super(appliedTax, refundTaxLines, refunded);
        }

        @Override
        protected Map<Integer, BigDecimal> getRefundingTaxLines(List<ShippingLine> shippingLines, List<RefundTaxLine> refundedTaxLines, Set<OrderAdjustment> refundItems) {
            Map<Integer, BigDecimal> refundingShippingTaxes = new LinkedHashMap<>();

            List<ShippingLine> shippingLinesHasTaxLine = CollectionUtils.isEmpty(shippingLines)
                    ? List.of()
                    : shippingLines.stream().filter(shippingLine -> CollectionUtils.isNotEmpty(shippingLine.getTaxLines())).toList();
            if (CollectionUtils.isEmpty(shippingLinesHasTaxLine)) {
                return refundingShippingTaxes;
            }

            var effectiveTaxes = shippingLinesHasTaxLine.stream()
                    .flatMap(shippingLine -> shippingLine.getTaxLines().stream())
                    .collect(Collectors.toMap(TaxLine::getId, TaxLine::getPrice, (m1, m2) -> m2, LinkedHashMap::new));

            for (var rfl : refundedTaxLines) {
                effectiveTaxes.computeIfPresent(rfl.getTaxLineId(), (k, ov) -> ov.subtract(rfl.getAmount()));
            }
            for (var refundItem : refundItems) {
                var refundAmount = refundItem.getTaxAmount();
                this.allocateRefundTaxAmount(refundingShippingTaxes, effectiveTaxes, refundAmount);
            }

            return refundingShippingTaxes;
        }
    }

    final static class RefundingLineItemTaxes extends RefundingTaxAbstract<LineItem, RefundLineItem> {

        RefundingLineItemTaxes(
                List<LineItem> appliedTax,
                List<RefundTaxLine> refundTaxLines,
                Set<RefundLineItem> refunded
        ) {
            super(appliedTax, refundTaxLines, refunded);
        }

        @Override
        protected Map<Integer, BigDecimal> getRefundingTaxLines(List<LineItem> lineItems, List<RefundTaxLine> refundedTaxLines, Set<RefundLineItem> refundLineItemTaxes) {
            Map<Integer, BigDecimal> refundingLineItemTaxes = new LinkedHashMap<>();

            for (var refundItem : refundLineItemTaxes) {
                var lineItemId = refundItem.getLineItemId();
                var lineItem = lineItems.stream()
                        .filter(line -> line.getId() == lineItemId)
                        .findFirst()
                        .orElseThrow();

                if (CollectionUtils.isEmpty(lineItem.getTaxLines())) {
                    continue;
                }

                var effectiveTaxLines = lineItem.getTaxLines().stream()
                        .collect(Collectors.toMap(TaxLine::getId, TaxLine::getPrice, (m1, m2) -> m2, LinkedHashMap::new));

                if (CollectionUtils.isNotEmpty(refundedTaxLines)) {
                    for (var rtl : refundedTaxLines) {
                        effectiveTaxLines.computeIfPresent(rtl.getTaxLineId(), (k, ov) -> ov.subtract(rtl.getAmount()));
                    }
                }

                for (var rtf : refundingLineItemTaxes.entrySet()) {
                    effectiveTaxLines.computeIfPresent(rtf.getKey(), (k, ov) -> ov.subtract(rtf.getValue()));
                }

                this.allocateRefundTaxAmount(refundingLineItemTaxes, effectiveTaxLines, refundItem.getTotalTax());
            }

            return refundingLineItemTaxes;
        }
    }

    @Getter
    static abstract class RefundingTaxAbstract<T, R> {

        private final Map<Integer, BigDecimal> refundingTaxes;

        RefundingTaxAbstract(List<T> appliedTax, List<RefundTaxLine> refundTaxLines, Set<R> refunded) {

            this.refundingTaxes = getRefundingTaxLines(appliedTax, refundTaxLines, refunded);
        }

        protected abstract Map<Integer, BigDecimal> getRefundingTaxLines(List<T> appliedTax, List<RefundTaxLine> refundTaxLines, Set<R> refund);

        protected void allocateRefundTaxAmount(Map<Integer, BigDecimal> refundingLineItemTaxes, LinkedHashMap<Integer, BigDecimal> effectiveTaxLines, BigDecimal totalTax) {
            // Phân bổ totalTax từ taxLine có id desc
            for (var iterator = new LinkedList<>(effectiveTaxLines.keySet()).descendingIterator(); iterator.hasNext(); ) {
                if (!NumberUtils.isPositive(totalTax)) {
                    break;
                }

                int taxLineId = iterator.next();
                var currentTaxAmount = effectiveTaxLines.get(taxLineId);
                if (!NumberUtils.isPositive(currentTaxAmount)) {
                    continue;
                }

                var refundTaxAmount = currentTaxAmount.min(totalTax);
                refundingLineItemTaxes.put(taxLineId, refundTaxAmount);

                totalTax = totalTax.subtract(refundTaxAmount);
            }
        }
    }

    private RefundResult buildRefund(Order order, RefundRequest refundRequest) {
        var suggestedRefund = this.refundCalculationService.calculateRefund(order, refundRequest);
        var refundLineItems = buildRefundLineItems(suggestedRefund);

        var refundTransactions = buildRefundTransactions(
                suggestedRefund, order, refundRequest.getTransactions()
        );

        var orderAdjustments = buildOrderAdjustments(
                suggestedRefund, refundLineItems, refundTransactions, order
        );

        var processedAt = refundRequest.getProcessedAt();

        var refund = new Refund(
                orderIdGenerator.generateRefundId(),
                refundLineItems,
                orderAdjustments,
                refundRequest.getNote(),
                processedAt
        );

        var refundedAmount = refundTransactions.stream()
                .map(OrderTransactionCreateRequest::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        refund.setTotalRefunded(refundedAmount);

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
            var shippingRefund = this.createShippingRefund(order);
            return shippingRefund != null ? Set.of(shippingRefund) : Set.of();
        }

        var maxRefundable = suggestedRefund.getTransactions().stream()
                .map(RefundCalculationResponse.Transaction::getMaximumRefundable)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        var totalRefundingAmount = refundTransactions.stream()
                .map(OrderTransactionCreateRequest::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        var refundAllMoney = maxRefundable.compareTo(totalRefundingAmount) == 0;

        var isFullRefund = refundAllMoney && refundAllItems;
        if (isFullRefund) {
            var adjustments = new HashSet<OrderAdjustment>();

            var shippingRefund = this.createShippingRefund(order);
            if (shippingRefund != null) adjustments.add(shippingRefund);

            var pastAdjustment = order.getRefunds().stream()
                    .flatMap(refund -> refund.getOrderAdjustments().stream())
                    .filter(adjustment -> adjustment.getKind() == OrderAdjustment.RefundKind.refund_discrepancy)
                    .map(OrderAdjustment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (pastAdjustment.compareTo(BigDecimal.ZERO) != 0) {
                var adjustmentId = this.orderIdGenerator.generateAdjustmentId();
                var fullRefundAdjustment = new OrderAdjustment();
                adjustments.add(fullRefundAdjustment);
            }

            return adjustments;
        }

        var adjustments = new HashSet<OrderAdjustment>();

        OrderAdjustment shippingAdjustment = null;
        var suggestedShippingRefund = suggestedRefund.getShipping();
        if (NumberUtils.isPositive(suggestedShippingRefund.getAmount())) {
            shippingAdjustment = this.createShippingRefund(suggestedShippingRefund, order.isTaxIncluded());
            adjustments.add(shippingAdjustment);
        }

        BigDecimal totalRefundingItemAmount;
        if (shippingAdjustment != null) {
            totalRefundingItemAmount = totalRefundingAmount
                    .add(shippingAdjustment.getAmount())
                    .add(shippingAdjustment.getTaxAmount());
        } else {
            totalRefundingItemAmount = totalRefundingAmount;
        }

        return adjustments;
    }

    private OrderAdjustment createShippingRefund(Order order) {
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
        return new OrderAdjustment();
    }

    private boolean isRefundAllItems(RefundCalculationResponse suggestedRefund, Set<RefundLineItem> refundLineItems) {
        if (CollectionUtils.isEmpty(suggestedRefund.getRefundableLineItems())) return true;

        var refundedLineMap = refundLineItems.stream()
                .collect(Collectors.groupingBy(
                        RefundLineItem::getLineItemId,
                        Collectors.reducing(
                                0,
                                RefundLineItem::getQuantity,
                                Integer::sum
                        )
                ));

        return suggestedRefund.getRefundableLineItems().stream()
                .allMatch(lineItem -> {
                    var refundLineQuantity = refundedLineMap.get(lineItem.getLineItemId());
                    return refundLineQuantity == lineItem.getMaximumRefundableQuantity();
                });
    }

    private List<OrderTransactionCreateRequest> buildRefundTransactions(
            RefundCalculationResponse suggestedRefund,
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
                        "require a parent_id"
                );
            }
        }

        var requestedAmount = refundTransactionRequests.stream()
                .map(OrderTransactionCreateRequest::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return refundTransactionRequests;
    }

    private Set<RefundLineItem> buildRefundLineItems(RefundCalculationResponse suggestedRefund) {
        var suggestedRefundLineItems = suggestedRefund.getRefundLineItems();
        if (CollectionUtils.isEmpty(suggestedRefundLineItems)) {
            return Set.of();
        }
        var ids = this.orderIdGenerator.generateReturnLineIds(suggestedRefundLineItems.size());
        return suggestedRefundLineItems.stream()
                .map(suggestedLineItem ->
                        new RefundLineItem(
                                ids.removeFirst(),
                                suggestedLineItem
                        ))
                .collect(Collectors.toSet());
    }

    record RefundResult(Refund refund, List<OrderTransactionCreateRequest> transactions) {
    }

    private Order findOrderById(OrderId orderId) {
        var order = this.orderRepository.findById(orderId);
        if (order != null) {
            return order;
        }
        throw new ConstrainViolationException("order", "not found");
    }

    private record ProductInfo(Map<Integer, VariantDto> variants, Map<Integer, ProductDto> products) {
    }

    private List<CombinationLine> buildCombinationLines(OrderCreateRequest request) {
        if (CollectionUtils.isEmpty(request.getCombinationLineRequests())) {
            return List.of();
        }

        var ids = this.orderIdGenerator.generateCombinationLineIds(request.getCombinationLineRequests().size());
        return request.getCombinationLineRequests()
                .stream()
                .map(lineRequest ->
                        new CombinationLine(
                                ids.removeFirst(),
                                lineRequest.getVariantId(),
                                lineRequest.getProductId(),
                                lineRequest.getPrice(),
                                lineRequest.getQuantity(),
                                lineRequest.getTitle(),
                                lineRequest.getVariantTitle(),
                                lineRequest.getSku(),
                                lineRequest.getVendor(),
                                lineRequest.getUnit(),
                                lineRequest.getItemUnit(),
                                lineRequest.getType()
                        ))
                .toList();
    }

    private Pair<BillingAddress, ShippingAddress> buildAddress(
            OrderCreateRequest.AddressRequest billingAddressRequest,
            OrderCreateRequest.AddressRequest shippingAddressRequest
    ) {
        BillingAddress billingAddress = null;
        ShippingAddress shippingAddress = null;
        if (billingAddressRequest != null) {
            var billingAddressId = this.orderIdGenerator.generateBillingAddressId();
            billingAddress = new BillingAddress(billingAddressId, resolveAddress(billingAddressRequest));
        }
        if (shippingAddressRequest != null) {
            var shippingAddressId = this.orderIdGenerator.generateShippingAddressId();
            shippingAddress = new ShippingAddress(shippingAddressId, resolveAddress(shippingAddressRequest));
        }
        return Pair.of(billingAddress, shippingAddress);
    }

    /**
     * return: Map: name - value
     */
    private Map<String, String> buildCustomAttributes(List<CustomAttributeRequest> noteAttributes) {
        if (CollectionUtils.isEmpty(noteAttributes)) return Map.of();
        return noteAttributes.stream()
                .filter(attribute -> StringUtils.isNotBlank(attribute.getName()))
                .collect(Collectors.toMap(
                        CustomAttributeRequest::getName,
                        CustomAttributeRequest::getValue,
                        (first, second) -> second));
    }

    private CustomerContextRequest resolveCustomer(int storeId, OrderCreateRequest request) {
        log.info("Resolving customer for storeId: {}, request: {}", storeId, request);

        var context = new CustomerContextRequest();

        // Nếu request không truyền lên bất kỳ thông tin nào của customer, email, phone => return null
        if (StringUtils.isBlank(request.getEmail())
                && StringUtils.isBlank(request.getPhone())
                && (request.getCustomer() == null || request.getCustomer().isEmpty())) {
            return context;
        }

        context.setEmail(request.getEmail());
        context.setPhone(request.getPhone());

        var reqCustomer = request.getCustomer();
        if (reqCustomer != null && reqCustomer.getId() > 0) {
            if (StringUtils.isNotBlank(reqCustomer.getEmail())) {
                context.setEmail(reqCustomer.getEmail());
            }
            if (StringUtils.isNotBlank(reqCustomer.getPhone())) {
                context.setPhone(reqCustomer.getPhone());
            }
        }

        var apiCustomer = this.customerService.findById(storeId, reqCustomer == null ? 0 : reqCustomer.getId());
        var emailCheckedCustomer = this.customerService.findByEmail(storeId, context.getEmail());
        validateEmailConflict(apiCustomer, emailCheckedCustomer, request, context);

        String updatablePhone = resolvePhoneNumber(context, storeId, apiCustomer);

        createOrderUpdateCustomer(storeId, apiCustomer, context, updatablePhone, request);

        return context;
    }

    private void createOrderUpdateCustomer(int storeId, Customer apiCustomer, CustomerContextRequest context, String updatablePhone, OrderCreateRequest request) {
        var address = request.getBillingAddress() == null ? null : resolveAddress(request.getBillingAddress());
        var fullName = address != null
                ? Pair.of(address.getFirstName(), address.getLastName())
                : Pair.<String, String>of(null, null);
        if (StringUtils.isNotBlank(context.getEmail())
                || StringUtils.isNotBlank(context.getPhone())
                || StringUtils.isNotBlank(fullName.getLeft())) {
            if (apiCustomer == null) {
                var contact = Pair.of(context.getEmail(), updatablePhone);
                apiCustomer = this.customerService.create(storeId, contact, fullName, address);
                context.setFirstTimeCustomer(true);
            } else {
                String finalEmail = Optional.ofNullable(apiCustomer.getEmail()).orElse(context.email);
                String finalPhone = Optional.ofNullable(apiCustomer.getPhone()).orElse(updatablePhone);
                var shouldUpdateCustomer = !StringUtils.equals(finalEmail, apiCustomer.getEmail())
                        || !StringUtils.equals(finalPhone, apiCustomer.getPhone());
                if (shouldUpdateCustomer) {
                    apiCustomer = this.customerService.update(storeId, apiCustomer.getId(), finalEmail, finalPhone);
                }
            }
        }
        if (apiCustomer != null) {
            context.setCustomerId(apiCustomer.getId());
            context.setAcceptsMarketing(request.isBuyerAcceptMarketing());
            if (StringUtils.isBlank(context.getEmail())) {
                context.setEmail(apiCustomer.getEmail());
            }
            if (StringUtils.isBlank(context.getPhone())) {
                context.setPhone(apiCustomer.getPhone());
            }
        }
    }

    private MailingAddress resolveAddress(OrderCreateRequest.AddressRequest address) {
        var names = extractFullName(address); // Get (firstName, lastName) from address
        var area = AddressHelper.resolve(this.orderMapper.toAddressRequest(address));
        return new MailingAddress(
                names.getLeft(),
                names.getRight(),
                address.getPhone(),
                address.getAddress1(),
                address.getAddress2(),
                address.getCompany(),
                area.getCountry(),
                area.getProvince(),
                area.getDistrict(),
                area.getWard()
        );
    }

    private Pair<String, String> extractFullName(OrderCreateRequest.AddressRequest address) {
        if (StringUtils.isNotBlank(address.getName())) {
            return AddressHelper.breakIntoStructureName(address.getName());
        }
        return Pair.of(address.getFirstName(), address.getLastName());
    }

    private String resolvePhoneNumber(CustomerContextRequest context, int storeId, Customer apiCustomer) {
        if (apiCustomer != null && StringUtils.isNotBlank(apiCustomer.getPhone())) {
            return null;
        }
        if (StringUtils.isBlank(context.getPhone()) || !CustomerPhoneUtils.isValid(context.getPhone())) {
            return null;
        }

        String normalizedPhone = CustomerPhoneUtils.normalize(context.getPhone());
        var phoneCheckedCustomer = this.customerService.findByPhone(storeId, normalizedPhone);

        if (phoneCheckedCustomer != null && (apiCustomer == null || apiCustomer.getId() != phoneCheckedCustomer.getId())) {
            log.warn("Phone number conflict detected: {}", phoneCheckedCustomer);
            return null;
        }

        return normalizedPhone;
    }

    private void validateEmailConflict(Customer apiCustomer, Customer emailCheckedCustomer, OrderCreateRequest request, CustomerContextRequest context) {
        if (emailCheckedCustomer == null) return;

        if (apiCustomer == null) {
            apiCustomer = emailCheckedCustomer;
            return;
        }

        if (apiCustomer.getId() == emailCheckedCustomer.getId()) return;

        if ("checkout".equals(request.getSource())) {
            log.warn("Checkout case detected: Keeping original email.");
            context.setEmail(apiCustomer.getEmail());
            if (!StringUtils.equals(request.getEmail(), apiCustomer.getEmail())) {
                request.setEmail(apiCustomer.getEmail());
            }
            return;
        }

        log.error("Email conflict detected for customer: {} and email: {}", apiCustomer, context.getEmail());
        throw new ConstrainViolationException("customer", "email has already been taken");
    }

    @Getter
    @Setter
    private static class CustomerContextRequest {
        private String email;
        private String phone;
        private Integer customerId;
        private boolean acceptsMarketing;
        private boolean firstTimeCustomer;
    }

    //region calculate tax
    private void validateTaxLineRequests(OrderCreateRequest orderRequest, Currency currency) {
        var isOrderHasTaxLines = CollectionUtils.isNotEmpty(orderRequest.getTaxLines());
        var isLineItemHasTaxLine = orderRequest.getLineItems().stream()
                .anyMatch(line -> CollectionUtils.isNotEmpty(line.getTaxLines()));

        //Truyền cả taxLines vào trong order hoặc lineItem => lỗi
        if (isOrderHasTaxLines && isLineItemHasTaxLine) {
            throw new ConstrainViolationException(UserError.builder()
                    .code("invalid")
                    .fields(List.of("order_tax_lines", "line_item_tax_lines"))
                    .message("Must fill taxLines in Order or in Order.LineItem only not both of them")
                    .build());
        }

        // shippingTaxLines
        if (CollectionUtils.isNotEmpty(orderRequest.getShippingLines())) {
            for (var shippingLine : orderRequest.getShippingLines()) {
                if (CollectionUtils.isEmpty(shippingLine.getTaxLines())) continue;
                var mergedTaxLines = this.mergeTaxLines(shippingLine.getTaxLines(), currency);
                shippingLine.setTaxLines(mergedTaxLines);
            }
        }

        //Gộp taxLines
        // Order
        if (isOrderHasTaxLines) {
            var validTaxLines = mergeTaxLines(orderRequest.getTaxLines(), currency);
            orderRequest.setTaxLines(validTaxLines);
        }
        // LineItem
        if (isLineItemHasTaxLine) {
            for (var lineItem : orderRequest.getLineItems()) {
                if (CollectionUtils.isEmpty(lineItem.getTaxLines())) continue;
                var mergedTaxLines = mergeTaxLines(lineItem.getTaxLines(), currency);
                lineItem.setTaxLines(mergedTaxLines);
            }
        }
    }

    private List<OrderCreateRequest.TaxLineRequest> mergeTaxLines(List<OrderCreateRequest.TaxLineRequest> taxLines, Currency currency) {
        Map<String, OrderCreateRequest.TaxLineRequest> mergedTaxLines = new HashMap<>();
        taxLines.forEach(taxLine -> {
            String taxLineKey = taxLine.getTitle() + "_" + taxLine.getRate();
            mergedTaxLines.compute(taxLineKey, (k, v) -> v == null ? taxLine : v.addPrice(taxLine.getPrice()));
        });
        return mergedTaxLines.values().stream()
                .peek(taxLine -> {
                    //NOTE: Làm tròn lại price sau khi merge
                    taxLine.setPrice(taxLine.getPrice()
                            .setScale(currency.getDefaultFractionDigits(), RoundingMode.UP));
                })
                .toList();
    }
    //endregion calculate tax

    private Pair<App, Channel> buildAppAndChannel(int storeId, String source, String sourceName, OrderCreateRequest orderRequest) {
        var apiKey = StringUtils.EMPTY;

        var isDraftOrder = "bizweb_draft_order".equals(source);

        var isApp = apiKey != null && !isDraftOrder;
        var isPrivateApp = isApp;

        var isCheckout = !isApp && "checkout".equals(source);
        var isOldCheckout = isCheckout && sourceName.equals("web");
        var isNewCheckout = isCheckout;

        if (!isApp && !isCheckout && !isDraftOrder) {
            return Pair.of(null, null);
        }

        String finalClientId;
        // Resolve clientId

        return null;
    }

    private TracingInfo buildTrackingInfo(Pair<String, String> sourceInfo, OrderCreateRequest orderRequest, String reference) {
        String source = sourceInfo.getKey();
        String sourceName = sourceInfo.getValue();
        String clientId = StringUtils.EMPTY;
        if (!orderRequest.isFromDraftOrder() && StringUtils.isNotBlank(clientId)) {
            //GET thông tin từ nơi khác
            if (log.isDebugEnabled()) {
                log.debug("GET FROM appConfig");
            }
        }

        return TracingInfo.builder()
                .source(source)
                .sourceName(sourceName)
                .cartToken(orderRequest.getCartToken())
                .checkoutToken(orderRequest.getCheckoutToken())
                .landingSite(orderRequest.getLandingSite())
                .landingSiteRef(orderRequest.getLandingSiteRef())
                .referringSite(orderRequest.getReferringSite())
                .reference(reference)
                .build();
    }

    private String buildReference(int storeId, OrderCreateRequest orderRequest) {
        var reference = orderRequest.getReference();
        if (StringUtils.isNotBlank(reference)) {
            var orderReference = this.orderDao.getByReference(storeId, reference);
            if (orderReference != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Order reference not found with reference = {}", reference);
                }
                reference = UUID.randomUUID().toString().replace("-", "");
            }
        }
        return reference;
    }

    private Pair<String, String> mapSources(String source, String sourceName) {
        var clientId = StringUtils.EMPTY;

        if (clientId != null) {
            if (StringUtils.isBlank(sourceName)) {
                return Pair.of(clientId, sourceName);
            } else if (StringUtils.isBlank(source)) {
                return Pair.of(clientId, source);
            }
        }
        if (StringUtils.isBlank(sourceName)) {
            sourceName = "web";
        }
        return Pair.of(source, sourceName);
    }

    private Currency resolveCurrency(String currencyRequest, StoreDto store) {
        if (StringUtils.isNotEmpty(currencyRequest)) {
            if (!Order.DEFAULT_CURRENCY.getCurrencyCode().equals(currencyRequest)) {
                if (checkSupportedCurrency(currencyRequest)) {
                    return Currency.getInstance(currencyRequest);
                }
            }
            return Order.DEFAULT_CURRENCY;
        }
        return StringUtils.isEmpty(store.getCurrency())
                ? Order.DEFAULT_CURRENCY
                : Currency.getInstance(store.getCurrency());
    }

    private boolean checkSupportedCurrency(String currencyCode) {
        var currency = SupportedCurrencies.getCurrency(currencyCode);
        if (currency != null) return true;
        throw new ConstrainViolationException("currency", "currency is not supported");
    }

    private StoreDto findStoreById(int storeId) {
        var store = storeDao.getStoreById(storeId);
        if (store != null) return store;
        throw new ConstrainViolationException("store", "store not found by id = " + storeId);
    }
}
