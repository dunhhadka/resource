package org.example.order.order.application.service.draftorder;

import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.example.order.order.infrastructure.configuration.exception.ConstrainViolationException;
import org.example.order.order.infrastructure.configuration.exception.ErrorMessage;
import org.example.order.order.infrastructure.configuration.exception.UserError;
import org.example.order.order.application.common.SupportedCurrencies;
import org.example.order.order.application.model.combination.request.CombinationCalculateRequest;
import org.example.order.order.application.model.draftorder.request.DraftAppliedDiscountRequest;
import org.example.order.order.application.model.draftorder.request.DraftLineItemRequest;
import org.example.order.order.application.model.draftorder.request.DraftOrderCreateRequest;
import org.example.order.order.application.service.combination.CombinationProcessor;
import org.example.order.order.application.utils.NumberUtils;
import org.example.order.order.application.utils.TaxLineUtils;
import org.example.order.order.domain.draftorder.model.*;
import org.example.order.order.domain.draftorder.persistence.DraftOrderIdGenerator;
import org.example.order.order.domain.draftorder.persistence.DraftOrderNumberGenerator;
import org.example.order.order.domain.order.model.Order;
import org.example.order.order.infrastructure.data.dao.ProductDao;
import org.example.order.order.infrastructure.data.dao.StoreDao;
import org.example.order.order.infrastructure.data.dto.ProductDto;
import org.example.order.order.infrastructure.data.dto.StoreDto;
import org.example.order.order.infrastructure.data.dto.VariantDto;
import org.example.order.order.infrastructure.persistence.DraftOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DraftOrderWriteService {

    private final DraftOrderIdGenerator draftOrderIdGenerator;
    private final DraftOrderNumberGenerator numberGenerator;

    private final DraftOrderRepository draftOrderRepository;

    private final DraftOrderMapper draftOrderMapper;

    private final StoreDao storeDao;
    private final ProductDao productDao;

    private final TaxHelper taxHelper;

    private final CombinationProcessor combinationProcessor;

    @Transactional
    public DraftOrderId createDraftOrder(int storeId, DraftOrderCreateRequest request) {
        var store = findStoreById(storeId);

        var draftId = new DraftOrderId(storeId, draftOrderIdGenerator.generateDraftOrderId());

        var currency = getCurrency(request.getCurrency(), store);

        var draftOrder = new DraftOrder(
                draftId,
                numberGenerator,
                request.getCopyOrderId(),
                currency,
                request.getUserId(),
                null,
                taxHelper
        );

        var lineItems = buildLineItems(storeId, request, currency, draftOrder);
        draftOrder.setLineItems(lineItems);

        return createOrUpdateDraftOrder(storeId, draftOrder, request);
    }

    private DraftOrderId createOrUpdateDraftOrder(int storeId, DraftOrder draftOrder, DraftOrderCreateRequest request) {
        buildDraftOrder(storeId, draftOrder, request);
        draftOrderRepository.save(draftOrder);
        return draftOrder.getId();
    }

    //NOTE: Set property for draftOrder
    private void buildDraftOrder(int storeId, DraftOrder draftOrder, DraftOrderCreateRequest request) {
        var draftOrderInfoBuilder = draftOrder.getDraftOrderInfo().toBuilder();
        draftOrderInfoBuilder
                .note(request.getNote())
                .email(request.getEmail())
                .phone(request.getPhone())
                .taxExempt(request.getTaxExempt())
                .locationId(request.getLocationId())
                .sourceName(request.getSourceName())
                .assigneeId(request.getAssigneeId());

        draftOrder.setDraftOrderInfo(draftOrderInfoBuilder.build());

        if (request.getAppliedDiscount() != null) {
            draftOrder.setAplliedDiscount(getDraftAppliedDiscount(request.getAppliedDiscount()));
        }

        if (request.getAppliedDiscount() != null && CollectionUtils.isNotEmpty(request.getLineItems())) {
            overrideOrderDiscountApplication(draftOrder);
        }

        //NOTE: gồm 2 case: cập nhật lineItems, chỉ cập nhật discount
        if (CollectionUtils.isNotEmpty(request.getLineItems())) {
            handleCombinations(storeId, draftOrder);
        }
    }

    private void handleCombinations(int storeId, DraftOrder draftOrder) {
        boolean allNorMalLine = draftOrder.getLineItems().stream()
                .allMatch(line -> line.getProductInfo().getType() == VariantType.normal);
        // Nếu chỉ có lineItem normal thì giữ nguyên không xử lý gì cả
        if (allNorMalLine)
            return;

        var draftLineItems = draftOrder.getLineItems();
        var currency = draftOrder.getDraftOrderInfo().getCurrency();
        var combinationCalculateRequest = CombinationCalculateRequest.builder()
                .currency(currency.getCurrencyCode())
                .calculateTax(true)
                .taxExempt(draftOrder.getDraftOrderInfo().getTaxExempt())
                .countryCode(TaxLineUtils.resolveCountryCode(draftOrder))
                .lineItems(this.draftOrderMapper.toCombinationLineItemRequests(draftOrder.getLineItems()))
                .build();
        var combinationResponse = this.combinationProcessor.calculate(storeId, combinationCalculateRequest);
        var errors = new HashMap<String, String>();
        for (int i = 0; i < draftLineItems.size(); i++) {
            var draftLineItem = draftLineItems.get(i);
            var calculateItem = combinationResponse.getLineItems().get(i);
            if (calculateItem.getType() != VariantType.normal && CollectionUtils.isEmpty(calculateItem.getComponents())) {
                errors.put("line_items[%s].variant_id".formatted(i),
                        ""
                );
                continue;
            }
            draftLineItem.setComponents(List.of());
        }
        if (MapUtils.isEmpty(errors)) {
            var errorMessageBuilder = ErrorMessage.builder();
            errors.forEach(errorMessageBuilder::addError);
            throw new ConstrainViolationException(errorMessageBuilder.build());
        }
        draftOrder.setLineItems(draftLineItems);
    }

    private void overrideOrderDiscountApplication(DraftOrder draftOrder) {
        for (var lineItem : draftOrder.getLineItems()) {
            lineItem.removeAllDiscountAllocations();
        }

    }

    private List<DraftLineItem> buildLineItems(int storeId, DraftOrderCreateRequest request, Currency currency, DraftOrder draftOrder) {

        List<DraftLineItem> lineItems = new ArrayList<>();

        List<DraftLineItemRequest> lineItemRequests = request.getLineItems();

        var productInfo = buildProductLineItemInfo(storeId, request.getLineItems());

        List<DraftDiscountApplication> discountApplications = new ArrayList<>();

        int applicationIndex = 0;
        var errors = new ArrayList<Pair<String, String>>();
        for (int i = 0; i < lineItemRequests.size(); i++) {
            var lineItemRequest = lineItemRequests.get(i);
            var productInfoBuilder = DraftProductInfo.builder()
                    .title(lineItemRequest.getTitle())
                    .vendor(lineItemRequest.getVendor())
                    .sku(lineItemRequest.getSku())
                    .taxable(lineItemRequest.isTaxable())
                    .variantId(null)
                    .productId(null);
            var shipInfoBuilder = DraftLineItemShipInfo.builder()
                    .quantity(lineItemRequest.getQuantity())
                    .grams(lineItemRequest.getGrams())
                    .requireShipping(lineItemRequest.isRequireShipping());

            if (lineItemRequest.isCustom()) {
                if (StringUtils.isBlank(lineItemRequest.getTitle())) {
                    errors.add(Pair.of("line_items[" + i + "].title", "can't be blank"));
                }
                if (lineItemRequest.getQuantity() <= 0) {
                    errors.add(Pair.of("line_items[" + i + "].quantity", "requires line item quantity"));
                }
                if (lineItemRequest.getPrice() == null || lineItemRequest.getPrice().compareTo(BigDecimal.ZERO) < 0) {
                    errors.add(Pair.of("line_items[" + i + "].price", "invalid price input"));
                } else {
                    productInfoBuilder.price(lineItemRequest.getPrice().setScale(currency.getDefaultFractionDigits(), RoundingMode.HALF_UP));
                }
            } else {
                var variant = productInfo.variants.get(lineItemRequest.getVariantId());
                if (variant == null) {
                    throw new IllegalArgumentException();
                }
                var product = productInfo.products.get(variant.getProductId());
                if (product == null) {
                    throw new IllegalArgumentException();
                }

                productInfoBuilder
                        .price(variant.getPrice().setScale(currency.getDefaultFractionDigits(), RoundingMode.HALF_UP))
                        .title(product.getName())
                        .vendor(product.getVendor())
                        .sku(variant.getSku())
                        .taxable(variant.isTaxable())
                        .variantTitle(variant.getTitle())
                        .variantId(variant.getId())
                        .inventoryItemId(variant.getInventoryItemId())
                        .inventoryPolicy(variant.getInventoryPolicy())
                        .inventoryManagement(variant.getInventoryManagement())
                        .productId(product.getId())
                        .unit(variant.getUnit())
                        .type(variant.getType());

                shipInfoBuilder
                        .grams(variant.getGrams())
                        .requireShipping(variant.isRequiresShipping());
            }

            //@formatter:off
            List<DraftProperty> properties = CollectionUtils.isNotEmpty(lineItemRequest.getProperties())
                    ? lineItemRequest.getProperties().stream()
                        .map(property -> DraftProperty.builder()
                                .name(property.getName())
                                .value(property.getValue())
                                .build()).toList()
                    : List.of();
            //@formatter:on

            var appliedDiscount = getDraftAppliedDiscount(lineItemRequest.getAppliedDiscount());

            //NOTE: discountAllocation được build ra từ appliedDiscount
            var lineItem = new DraftLineItem(
                    productInfoBuilder.build(),
                    shipInfoBuilder.build(),
                    appliedDiscount,
                    properties
            );

            if (appliedDiscount != null) {
                var discountInfo = resolveDiscount(lineItem, applicationIndex);
                lineItem.setDiscountAllocations(discountInfo.getValue());
                applicationIndex++;
                discountApplications.add(discountInfo.getKey());
            }
            lineItems.add(lineItem);
        }

        draftOrder.setDiscountApplications(discountApplications);

        if (!errors.isEmpty()) {
            var errorMessageBuilder = ErrorMessage.builder();
            for (var error : errors) {
                errorMessageBuilder.addError(error.getKey(), error.getValue());
            }
            throw new ConstrainViolationException(errorMessageBuilder.build());
        }

        return lineItems;
    }

    private Pair<DraftDiscountApplication, List<DraftDiscountAllocation>> resolveDiscount(DraftLineItem lineItem, int index) {
        var appliedDiscount = lineItem.getAppliedDiscount();
        if (appliedDiscount == null) return Pair.of(null, List.of());

        var valueType = DraftDiscountApplication.ValueType.valueOf(appliedDiscount.getValueType().name());

        var application = DraftDiscountApplication.builder()
                .index(index)
                .targetType(DraftDiscountApplication.TargetType.line_item)
                .valueType(valueType)
                .value(appliedDiscount.getValue())
                .description(appliedDiscount.getDescription())
                .amount(appliedDiscount.getAmount())
                .code(appliedDiscount.getCode())
                .build();
        var allocation = DraftDiscountAllocation.builder()
                .amount(appliedDiscount.getAmount())
                .discountApplicationIndex(index)
                .build();

        return Pair.of(application, List.of(allocation));
    }

    private DraftAppliedDiscount getDraftAppliedDiscount(DraftAppliedDiscountRequest discountRequest) {
        if (discountRequest == null) return null;
        return DraftAppliedDiscount.builder()
                .title(discountRequest.getTitle())
                .description(discountRequest.getDescription())
                .value(discountRequest.getValue())
                .valueType(discountRequest.getValueType())
                .custom(discountRequest.isCustom())
                .build();
    }

    private ProductInfo buildProductLineItemInfo(int storeId, List<DraftLineItemRequest> lineItems) {
        var variantIds = lineItems.stream()
                .map(DraftLineItemRequest::getVariantId)
                .filter(NumberUtils::isPositive)
                .distinct()
                .toList();
        var variants = productDao.findVariantByListId(storeId, variantIds).stream()
                .collect(Collectors.toMap(VariantDto::getId, Function.identity()));
        String variantNotFound = variantIds.stream()
                .filter(id -> !variants.containsKey(id))
                .map(String::valueOf)
                .collect(Collectors.joining(", "));
        if (StringUtils.isNotEmpty(variantNotFound)) {
            throw new ConstrainViolationException(UserError.builder()
                    .code("invalid")
                    .message("variants not found: " + variantNotFound)
                    .fields(List.of("variant_id"))
                    .build());
        }

        var productIds = variants.values().stream()
                .map(VariantDto::getProductId)
                .distinct().toList();
        var products = productDao.findProductByListId(storeId, productIds)
                .stream().collect(Collectors.toMap(ProductDto::getId, Function.identity()));

        return new ProductInfo(variants, products);
    }

    private record ProductInfo(Map<Integer, VariantDto> variants, Map<Integer, ProductDto> products) {
    }

    private StoreDto findStoreById(int storeId) {
        var store = storeDao.getStoreById(storeId);
        if (store == null)
            throw new ConstrainViolationException("store", "store not found by id = " + storeId);
        return store;
    }

    private Currency getCurrency(String currencyCode, StoreDto store) {
        if (StringUtils.isNotEmpty(currencyCode)) {
            if (!Order.DEFAULT_CURRENCY.getCurrencyCode().equals(currencyCode)) {
                checkSupportedCurrency(currencyCode);
                return Currency.getInstance(currencyCode);
            }
            return Order.DEFAULT_CURRENCY;
        }
        return store.getCurrency() != null ? Currency.getInstance(store.getCurrency()) : Order.DEFAULT_CURRENCY;
    }

    private void checkSupportedCurrency(String currencyCode) {
        var currency = SupportedCurrencies.getCurrency(currencyCode);
        if (currency == null) {
            throw new ConstrainViolationException(
                    "currency",
                    "not supported"
            );
        }
    }
}
