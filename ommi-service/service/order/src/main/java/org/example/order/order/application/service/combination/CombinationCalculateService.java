package org.example.order.order.application.service.combination;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.example.order.order.infrastructure.configuration.exception.ConstrainViolationException;
import org.example.order.order.infrastructure.configuration.exception.ErrorMessage;
import org.example.order.order.infrastructure.configuration.exception.UserError;
import org.example.order.order.application.model.combination.request.CombinationCalculateRequest;
import org.example.order.order.application.model.combination.request.CombinationLineItemRequest;
import org.example.order.order.application.model.combination.request.ComboPacksizeDiscountAllocations;
import org.example.order.order.application.model.combination.response.*;
import org.example.order.order.application.model.draftorder.TaxSettingValue;
import org.example.order.order.application.model.draftorder.response.CalculateProductInfo;
import org.example.order.order.application.service.draftorder.*;
import org.example.order.order.application.utils.NumberUtils;
import org.example.order.order.application.utils.TaxLineUtils;
import org.example.order.order.domain.draftorder.model.VariantType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CombinationCalculateService extends CombinationProcessor {

    private final SapoClient sapoClient;
    private final CombinationMapper combinationMapper;
    private final TaxHelper taxHelper;

    @Override
    protected CombinationCalculateResponse calculateWithoutUpdateProduct(CombinationCalculateRequest request, CalculateProductInfo productInfo) {
        var lineItems = new ArrayList<CombinationLineItemResponse>();
        for (var lineItem : request.getLineItems()) {
            var lineItemResponse = this.combinationMapper.toLineItemResponse(lineItem);
            var components = lineItem.getComponents().stream().map(combinationMapper::toLineItemComponent).toList();
            if (CollectionUtils.isNotEmpty(components)) {
                var lineItemPrice = lineItemResponse.getLinePrice();
                var minQuantity = components.stream().map(CombinationLineItemComponent::getQuantity).min(Comparator.naturalOrder()).orElse(productInfo.getRemainderUnit());
                for (var component : components) {
                    component.setLinePrice(component.getLinePrice());
                }
                var canBeOddComponent = components.stream().filter(CombinationLineItemComponent::isCanBeOdd).findFirst().orElse(components.get(0));
                var sortComponents = components.stream()
                        .sorted(Comparator.comparing(CombinationLineItemComponent::getRemainder).reversed()).toList();
                handleDiscountAllocations(canBeOddComponent, components, sortComponents, lineItem.getDiscountAllocations(), lineItemPrice, productInfo, minQuantity);
            }
            if (request.isCalculateTax()) {
                handleTaxLines(lineItemResponse, productInfo.getCountryTax(), productInfo.getCurrency(), request.isTaxIncluded(), request.isTaxExempt(), productInfo);
            }
            lineItems.add(lineItemResponse);
        }
        return CombinationCalculateResponse.builder()
                .lineItems(lineItems)
                .build();
    }

    /**
     * Tính toán có cập nhật lại thông tin mới nhất của sản phẩm
     */
    @Override
    protected CombinationCalculateResponse calculate(CombinationCalculateRequest request, CalculateProductInfo productInfo) {
        var lineItemRequests = request.getLineItems();
        var variantMap = productInfo.getVariantMap();
        var productMap = productInfo.getProductMap();
        var packsizeMap = productInfo.getPacksizeMap();
        var currency = productInfo.getCurrency();

        List<CombinationLineItemResponse> lineItemResponses = new ArrayList<>();
        for (var lineItemRequest : lineItemRequests) {
            var lineItemQuantity = lineItemRequest.getQuantity();
            /**
             * Sản phẩm custom vẫn giữ nguyên tính toán cũ
             * */
            if (lineItemRequest.getVariantId() == null) {
                var customLineItem = combinationMapper.toLineItemResponse(lineItemRequest);
                var lineItemPrice = lineItemQuantity.multiply(lineItemRequest.getPrice());
                customLineItem.setLinePrice(lineItemPrice);
                lineItemResponses.add(customLineItem);
                continue;
            }
            var variant = variantMap.get(lineItemRequest.getVariantId());
            if (variant == null) {
                throw new ConstrainViolationException(UserError.builder()
                        .code("not_found")
                        .fields(List.of("variant_id"))
                        .message("variant not found with id = %s".formatted(lineItemRequest.getVariantId()))
                        .build());
            }
            var product = productMap.get(lineItemRequest.getProductId());
            // update lại thông tin line
            var lineItem = this.combinationMapper.toResponse(lineItemRequest, variant, product);
            lineItemResponses.add(lineItem);

            //Giá của tổng line = quantity * price của line gốc
            BigDecimal lineItemPrice;

            //NOTE: Nếu có truyền cả variantId và price thì ưu tiên lấy price
            if (lineItemRequest.getPrice() != null && lineItemRequest.getPrice().signum() >= 0) {
                lineItem.setPrice(lineItemRequest.getPrice());
                lineItemPrice = lineItemQuantity.multiply(lineItem.getPrice());
            } else {
                lineItemPrice = lineItemQuantity.multiply(variant.getPrice());
            }
            lineItem.setLinePrice(lineItemPrice);

            // phân bổ giá về line thành phần
            switch (lineItem.getType()) {
                case packsize -> {
                    var packsize = packsizeMap.get(variant.getId());
                    var childProduct = productMap.get(packsize.getProductId());
                    var childVariant = variantMap.get(packsize.getVariantId());
                    if (childVariant == null) {
                        if (log.isDebugEnabled()) {
                            log.debug("child variant of packsize is null");
                        }
                        lineItem.setComponents(List.of());
                        continue;
                    }
                    // Tổng số lượng tất cả các component của packsize
                    var quantity = packsize.getQuantity().multiply(lineItemQuantity);
                    // Giá của 1 quantity khi phân bổ giá về components
                    var componentPrice = lineItemPrice.divide(quantity, currency.getDefaultFractionDigits(), RoundingMode.FLOOR);
                    // Phần dư khi phân bổ giá
                    var remainder = lineItemPrice.subtract(componentPrice.multiply(quantity));

                    var lineItemComponent = this.combinationMapper.toLineItemComponent(
                            childProduct,
                            childVariant,
                            quantity,
                            packsize.getQuantity(),
                            componentPrice,
                            lineItemPrice,
                            remainder,
                            VariantType.packsize
                    );
                    if (NumberUtils.isPositive(remainder)) {
                        lineItemComponent.setCanBeOdd(true);
                    }
                    lineItem.setComponents(List.of(lineItemComponent));
                }
                case combo -> {
                    var components = buildComboComponents(variant, lineItemPrice, lineItemQuantity, productInfo, lineItemRequest.getDiscountAllocations());
                }
            }
        }
        if (request.isCalculateTax()) {
            var countryTax = productInfo.getCountryTax();
            for (var lineItem : lineItemResponses) {
                handleTaxLines(
                        lineItem,
                        countryTax,
                        currency,
                        request.isTaxIncluded(),
                        request.isTaxExempt(),
                        productInfo
                );
            }
        }
        return CombinationCalculateResponse.builder()
                .lineItems(lineItemResponses)
                .build();
    }

    private void handleTaxLines(
            CombinationLineItemResponse lineItem,
            TaxSettingValue countryTax,
            Currency currency,
            boolean taxIncluded,
            boolean taxExempt,
            CalculateProductInfo productInfo
    ) {
        List<ComboPacksizeTaxLineResponse> taxLines = new ArrayList<>();
        Map<String, ComboPacksizeTaxLineResponse> taxLineMap = new HashMap<>();
        var hasCustomTax = CollectionUtils.isNotEmpty(lineItem.getTaxLines());
        if (hasCustomTax) {
            taxLineMap = TaxLineUtils.merge(lineItem.getTaxLines());
            taxLines = taxLineMap.values().stream().toList();
        }
        switch (lineItem.getType()) {
            case combo, packsize -> {
                var components = lineItem.getComponents();
                if (hasCustomTax) {
                    var count = components.size();
                    for (var taxLine : taxLines) {
                        var appliedTaxPrice = BigDecimal.ZERO;
                        for (int i = 0; i < count; i++) {
                            var component = components.get(i);
                            if (i < count - 1) {
                                var price = TaxLineUtils.distribute(taxLine.getPrice(), lineItem.getLinePrice(), component.getLinePrice(), currency);
                                appliedTaxPrice = appliedTaxPrice.add(price);
                                var customTaxLine = ComboPacksizeTaxLineResponse.builder()
                                        .rate(taxLine.getRate())
                                        .title(taxLine.getTitle())
                                        .price(price)
                                        .build();
                                component.addTaxLine(customTaxLine);
                                continue;
                            }
                            component.addTaxLine(ComboPacksizeTaxLineResponse.builder()
                                    .rate(taxLine.getRate())
                                    .title(taxLine.getTitle())
                                    .price(taxLine.getPrice().subtract(appliedTaxPrice))
                                    .build()
                            );
                        }
                    }
                } else if (!taxExempt) {
                    for (var component : components) {
                        if (!component.isTaxable()) continue;
                        var productTax = productInfo.getProductTaxMap().get(component.getProductId());
                        var taxLine = TaxLineUtils.buildTaxLine(productTax, countryTax, component.getSubtotal(), currency, taxIncluded);
                        component.setTaxLines(List.of(taxLine));
                    }
                    var calculatedTaxLines = components.stream()
                            .flatMap(component -> component.getTaxLines().stream())
                            .collect(TaxLineUtils.merge());
                    lineItem.setTaxLines(calculatedTaxLines);
                }
            }
            case normal -> {
                if (!hasCustomTax && !taxExempt && lineItem.isTaxable()) {
                    var productTax = lineItem.getProductId() == null
                            ? null
                            : productInfo.getProductTaxMap().get(lineItem.getProductId());
                    taxLines.add(TaxLineUtils.buildTaxLine(productTax, countryTax, lineItem.getLinePrice(), currency, taxIncluded));
                    lineItem.setTaxLines(taxLines);
                }
            }
        }
    }

    private List<CombinationLineItemComponent> buildComboComponents(
            VariantResponse.Variant variant,
            BigDecimal lineItemPrice,
            BigDecimal lineItemQuantity,
            CalculateProductInfo productInfo,
            List<@Valid ComboPacksizeDiscountAllocations> discountAllocations
    ) {
        var combo = productInfo.getComboMap().get(variant.getId());
        var comboItems = combo.getComboItems();
        if (CollectionUtils.isEmpty(comboItems)) {
            if (log.isDebugEnabled()) {
                log.debug("ComboItems of VariantId = {} is empty", variant.getId());
            }
            return List.of();
        }

        var currency = productInfo.getCurrency();

        var originalComboPrice = comboItems.stream()
                .map(item ->
                        item.getPrice().multiply(item.getQuantity())
                                .setScale(currency.getDefaultFractionDigits(), RoundingMode.HALF_UP))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        //Các comboItem có Price > 0
        var positivePriceComboItems = comboItems.stream().filter(item -> NumberUtils.isPositive(item.getPrice())).toList();
        var positiveCount = positivePriceComboItems.size();
        var positiveItemQuantity = positivePriceComboItems.stream().map(ComboItem::getQuantity).reduce(BigDecimal.ZERO, BigDecimal::add);

        // Các comboItems có Price = 0
        var zeroPriceComboItems = comboItems.stream().filter(item -> !NumberUtils.isPositive(item.getPrice())).toList();
        var zeroCount = zeroPriceComboItems.size();
        var zeroItemQuantity = zeroPriceComboItems.stream().map(ComboItem::getQuantity).reduce(BigDecimal.ZERO, BigDecimal::add);

        List<CombinationLineItemComponent> components = new ArrayList<>();

        // Quantity nhỏ nhất của component: Dùng để phân bổ remainder
        BigDecimal minLineQuantity = lineItemQuantity.multiply(comboItems.get(0).getQuantity());

        // Phân bổ giá về các line có price > 0
        BigDecimal appliedPrice = BigDecimal.ZERO;
        for (int i = 0; i < positivePriceComboItems.size(); i++) {
            var comboItem = positivePriceComboItems.get(i);
            var childProduct = productInfo.getProductMap().get(comboItem.getProductId());
            var component = buildComboComponent(
                    comboItem,
                    childProduct,
                    lineItemQuantity,
                    lineItemPrice,
                    appliedPrice,
                    i == positiveCount - 1,
                    currency,
                    false,
                    originalComboPrice,
                    positiveItemQuantity
            );
            components.add(component);
            appliedPrice = appliedPrice.add(component.getLinePrice())
                    .add(component.getRemainder());
            if (component.getQuantity().compareTo(minLineQuantity) > 0) minLineQuantity = component.getQuantity();
        }

        // Phân bổ giá về các line có price = 0
        for (int i = 0; i < zeroCount; i++) {
            var comboItem = zeroPriceComboItems.get(i);
            var childProduct = productInfo.getProductMap().get(comboItem.getProductId());
            var component = buildComboComponent(
                    comboItem,
                    childProduct,
                    lineItemQuantity,
                    lineItemPrice,
                    BigDecimal.ZERO,
                    i == zeroCount - 1,
                    currency,
                    true,
                    BigDecimal.ZERO,
                    zeroItemQuantity
            );
            components.add(component);
            if (component.getQuantity().compareTo(minLineQuantity) > 0) minLineQuantity = component.getQuantity();
        }

        var totalRemainder = components.stream().map(CombinationLineItemComponent::getRemainder).reduce(BigDecimal.ZERO, BigDecimal::add);

        var sortedComponents = components.stream().sorted(Comparator.comparing(CombinationLineItemComponent::getRemainder)).toList();
        var canBeOddComponent = addRemainder(totalRemainder, minLineQuantity, productInfo, sortedComponents);
        handleDiscountAllocations(canBeOddComponent, components, sortedComponents, discountAllocations, lineItemPrice, productInfo, minLineQuantity);
        return components;
    }

    private void handleDiscountAllocations(
            CombinationLineItemComponent canBeOddComponent,
            List<CombinationLineItemComponent> components,
            List<CombinationLineItemComponent> sortedComponents,
            List<ComboPacksizeDiscountAllocations> discountAllocations,
            BigDecimal lineItemPrice,
            CalculateProductInfo productInfo,
            BigDecimal minLineQuantity
    ) {
        if (CollectionUtils.isEmpty(discountAllocations)) return;
        var currency = productInfo.getCurrency();
        var componentCount = components.size();
        var lastLineHasPriceIndex = componentCount - 1;
        for (int i = componentCount - 1; i > 0; i--) {
            if (NumberUtils.isPositive(components.get(i).getPrice())) {
                lastLineHasPriceIndex = i;
                break;
            }
        }
        for (var discountAllocation : discountAllocations) {
            var discountAmount = discountAllocation.getAmount();
            var appliedDiscountAmount = BigDecimal.ZERO;
            var totalRemainder = BigDecimal.ZERO;
            for (int i = 0; i < components.size(); i++) {
                var component = components.get(i);
                var splitAmount = lastLineHasPriceIndex == i
                        ? discountAmount.subtract(appliedDiscountAmount)
                        : component.getLinePrice().compareTo(BigDecimal.ZERO) == 0
                        ? BigDecimal.ZERO
                        : component.getLinePrice().multiply(discountAmount).divide(lineItemPrice, currency.getDefaultFractionDigits(), RoundingMode.HALF_UP);
                appliedDiscountAmount = appliedDiscountAmount.add(splitAmount);
                var amount = splitAmount.min(component.getLinePrice()).divide(component.getQuantity(), currency.getDefaultFractionDigits(), RoundingMode.FLOOR)
                        .multiply(component.getQuantity());
                var remainder = splitAmount.subtract(amount);
                totalRemainder = totalRemainder.add(remainder);
                var allocation = ComboPacksizeDioscuntAllocationResponse.builder()
                        .discountApplicationId(discountAllocation.getApplicationId())
                        .amount(amount)
                        .remainder(remainder)
                        .build();
                component.addDiscountAllocations(allocation);

                // replace discountRemainder mới luôn
                component.setDiscountRemainder(allocation);
            }

            var sortedComponentByDiscountRemainder = components.stream().sorted(_REMAINDER).toList();

            // Chia phần dư vào các discountAllocations
            addRemainderForDiscountAllocations(
                    canBeOddComponent,
                    sortedComponents,
                    sortedComponentByDiscountRemainder,
                    totalRemainder,
                    minLineQuantity,
                    productInfo
            );
        }
    }

    private void addRemainderForDiscountAllocations(
            CombinationLineItemComponent canBeOddComponent,
            List<CombinationLineItemComponent> components,
            List<CombinationLineItemComponent> sortedComponentByDiscountRemainder,
            BigDecimal totalRemainder,
            BigDecimal minLineQuantity,
            CalculateProductInfo productInfo
    ) {
        if (totalRemainder.compareTo(BigDecimal.ZERO) == 0) return;
        var remainderUnit = productInfo.getRemainderUnit();
        for (var component : components) {
            var discountAllocation = component.getDiscountRemainder();
            var addPrice = component.getQuantity().multiply(remainderUnit);
            addPrice = addPrice.min(component.getSubtotal());
            var newSubtotal = component.getSubtotal().subtract(addPrice);
            if (addPrice.compareTo(totalRemainder) <= 0 && newSubtotal.compareTo(BigDecimal.ZERO) >= 0) {
                discountAllocation.addAmount(addPrice);
                totalRemainder = totalRemainder.subtract(addPrice);
                component.setSubtotal(newSubtotal);
            }
            if (totalRemainder.compareTo(BigDecimal.ZERO) <= 0) {
                return;
            }
            if (totalRemainder.compareTo(minLineQuantity.multiply(remainderUnit)) < 0)
                break;
        }

        var oddComponentDiscount = canBeOddComponent.getDiscountRemainder();
        var aPrice = totalRemainder.min(canBeOddComponent.getSubtotal());
        oddComponentDiscount.addAmount(aPrice);
        totalRemainder = totalRemainder.subtract(aPrice);
        canBeOddComponent.setSubtotal(canBeOddComponent.getSubtotal().subtract(aPrice));

        if (totalRemainder.compareTo(BigDecimal.ZERO) > 0) {
            for (var component : sortedComponentByDiscountRemainder) {
                var discountAllocation = component.getDiscountRemainder();
                var addPrice = component.getSubtotal().min(totalRemainder);
                var newSubtotal = component.getSubtotal().subtract(addPrice);
                discountAllocation.addAmount(addPrice);
                component.setSubtotal(newSubtotal);
                totalRemainder = totalRemainder.subtract(addPrice);
                if (totalRemainder.compareTo(BigDecimal.ZERO) <= 0)
                    return;
            }
        }
    }

    private static final Comparator<CombinationLineItemComponent> _REMAINDER = Comparator
            .comparing(CombinationLineItemComponent::getDiscountRemainder, (remainder1, remainder2) -> {
                if (remainder1 == null && remainder2 == null) return 0;
                if (remainder1 == null) return 1;
                if (remainder2 == null) return -1;
                return remainder1.getRemainder().compareTo(remainder2.getRemainder());
            });

    private CombinationLineItemComponent addRemainder(
            BigDecimal totalRemainder,
            BigDecimal minLineQuantity,
            CalculateProductInfo productInfo,
            List<CombinationLineItemComponent> components
    ) {
        if (BigDecimal.ZERO.compareTo(totalRemainder) > 0) return components.get(0);
        //NOTE: Nếu phần dư < minLineQuantity thì cộng vào line có dư cao nhất chưa được chia lần nào
        var remainderUnit = productInfo.getRemainderUnit();
        for (var component : components) {
            var addPrice = component.getQuantity().multiply(remainderUnit);
            if (addPrice.compareTo(totalRemainder) <= 0) {
                component.setLinePrice(component.getLinePrice().add(addPrice));
                component.setPrice(component.getPrice().add(remainderUnit));
                totalRemainder = totalRemainder.subtract(addPrice);
            }
            if (totalRemainder.compareTo(BigDecimal.ZERO) <= 0) {
                component.setCanBeOdd(true);
                return component;
            }
            if (totalRemainder.compareTo(minLineQuantity.multiply(remainderUnit)) <= 0) break;
        }

        if (totalRemainder.compareTo(BigDecimal.ZERO) > 0) {
            var currency = productInfo.getCurrency();
            for (var component : components) {
                if (!component.isChanged()) {
                    var newLinePrice = component.getLinePrice().add(totalRemainder);
                    component.setLinePrice(newLinePrice);
                    component.setPrice(newLinePrice.divide(component.getQuantity(), currency.getDefaultFractionDigits(), RoundingMode.FLOOR));
                    component.setCanBeOdd(true);
                    return component;
                }
            }
        }

        var canBeOddComponent = components.get(0);
        canBeOddComponent.setCanBeOdd(true);
        return canBeOddComponent;
    }

    private CombinationLineItemComponent buildComboComponent(
            ComboItem comboItem,
            ProductResponse.Product childProduct,
            BigDecimal lineItemQuantity,
            BigDecimal lineItemPrice,
            BigDecimal appliedPrice,
            boolean isLastLine,
            Currency currency,
            boolean isAvgSplit,
            BigDecimal originalComboPrice,
            BigDecimal totalItemCount
    ) {
        var componentQuantity = comboItem.getQuantity().multiply(lineItemQuantity);
        var componentBuilder = CombinationLineItemComponent.builder()
                .variantId(comboItem.getVariantId())
                .productId(childProduct.getId())
                .inventoryItemId(comboItem.getInventoryItemId())
                .sku(comboItem.getSku())
                .title(childProduct.getName())
                .variantTitle(comboItem.getTitle())
                .vendor(childProduct.getVendor())
                .unit(comboItem.getUnit())
                .inventoryManagement(comboItem.getInventoryManagement())
                .inventoryPolicy(comboItem.getInventoryPolicy())
                .grams(comboItem.getGrams())
                .requireShipping(comboItem.isRequiresShipping())
                .taxable(comboItem.isTaxable())
                .quantity(componentQuantity)
                .baseQuantity(comboItem.getQuantity())
                .type(VariantType.combo);

        BigDecimal itemUnitPrice;
        BigDecimal componentLinePrice;
        BigDecimal remainder;
        if (!isLastLine) {
            // Giá dược phân bổ về line
            var splitPrice = isAvgSplit || BigDecimal.ZERO.equals(originalComboPrice)
                    ? lineItemPrice
                    .multiply(comboItem.getQuantity())
                    .divide(totalItemCount, currency.getDefaultFractionDigits(), RoundingMode.HALF_UP)
                    : comboItem.getPrice()
                    .multiply(componentQuantity).multiply(lineItemPrice)
                    .divide(originalComboPrice, currency.getDefaultFractionDigits(), RoundingMode.HALF_UP);

            // Giá của 1 quantity
            itemUnitPrice = splitPrice.divide(componentQuantity, currency.getDefaultFractionDigits(), RoundingMode.FLOOR);
            // Giá của tổng line
            componentLinePrice = itemUnitPrice.multiply(componentQuantity);
            // Phần dư có thể có
            remainder = splitPrice.subtract(componentLinePrice);
        } else {
            // Xử lý line cuối
            var splitPrice = originalComboPrice.subtract(appliedPrice);
            itemUnitPrice = splitPrice.divide(componentQuantity, currency.getDefaultFractionDigits(), RoundingMode.FLOOR);
            componentLinePrice = itemUnitPrice.multiply(componentQuantity);
            remainder = splitPrice.subtract(componentLinePrice);
        }
        return componentBuilder
                .price(itemUnitPrice)
                .linePrice(componentLinePrice)
                .remainder(remainder)
                .canBeOdd(NumberUtils.isPositive(remainder))
                .build();
    }

    @Override
    protected CalculateProductInfo getProductInfo(int storeId, CombinationCalculateRequest request) {
        var productInfoBuilder = CalculateProductInfo.builder();
        var variantIds = request.getLineItems().stream()
                .filter(line -> !line.isCustom())
                .map(CombinationLineItemRequest::getVariantId)
                .filter(NumberUtils::isPositive)
                .distinct().toList();
        if (CollectionUtils.isEmpty(variantIds))
            return productInfoBuilder.build();
        var variants = sapoClient.variantFilter(storeId, variantIds).getVariants();
        var variantMap = variants.stream().collect(Collectors.toMap(VariantResponse.Variant::getId, Function.identity()));
        var allProductIds = variants.stream().map(VariantResponse.Variant::getProductId).collect(Collectors.toList());
        var comboVariantIds = variants.stream()
                .filter(variant -> variant.getType() == VariantType.packsize)
                .map(VariantResponse.Variant::getId)
                .toList();
        var packsizeIds = variants.stream()
                .filter(variant -> variant.getType() == VariantType.packsize)
                .map(VariantResponse.Variant::getId)
                .toList();
        if (CollectionUtils.isNotEmpty(comboVariantIds)) {
            var combos = sapoClient.comboFilter(storeId, ComboFilter.builder().variantIds(comboVariantIds).build()).getCombos();
            productInfoBuilder.comboMap(combos.stream().collect(Collectors.toMap(Combo::getVariantId, Function.identity())));

            Map<Integer, VariantResponse.Variant> comboVariantMap = new HashMap<>();
            combos.stream().flatMap(combo -> combo.getComboItems().stream())
                    .forEach(item -> comboVariantMap.putIfAbsent(item.getVariantId(), combinationMapper.toVariant(item)));
            variantMap.putAll(comboVariantMap);

            allProductIds.addAll(comboVariantMap.values().stream().map(VariantResponse.Variant::getProductId).distinct().toList());
        }
        if (CollectionUtils.isNotEmpty(packsizeIds)) {
            var packsizes = sapoClient.packsizeFilter(storeId, packsizeIds).getPacksizes();
            productInfoBuilder.packsizeMap(packsizes.stream().collect(Collectors.toMap(Packsize::getId, Function.identity())));
            var packsizeVariantIds = packsizes.stream().map(Packsize::getPacksizeVariantId).distinct().toList();
            var packsizeVariantMap = sapoClient.variantFilter(storeId, packsizeVariantIds).getVariants()
                    .stream().collect(Collectors.toMap(VariantResponse.Variant::getId, Function.identity()));
            variantMap.putAll(packsizeVariantMap);
            allProductIds.addAll(packsizeVariantMap.values().stream().map(VariantResponse.Variant::getProductId).distinct().toList());
        }

        productInfoBuilder.variantMap(variantMap);

        var productIds = allProductIds.stream().distinct().toList();
        var productMap = sapoClient.productFilter(storeId, productIds).getProducts().stream()
                .collect(Collectors.toMap(ProductResponse.Product::getId, Function.identity()));
        productInfoBuilder.productMap(productMap);

        productInfoBuilder.currency(Currency.getInstance(request.getCurrency()));
        productInfoBuilder.remainderUnit(BigDecimal.ONE.movePointRight(2));

        if (request.isCalculateTax()) {
            String countryCode = request.getCountryCode();
            var taxSetting = taxHelper.getTaxSetting(storeId, countryCode, productMap.keySet(), false);
            var countryTax = taxSetting.getTaxes().stream()
                    .filter(taxValue -> !NumberUtils.isPositive(taxValue.getProductId()))
                    .findFirst().orElse(TaxSettingValue.builder().rate(BigDecimal.ZERO).build());
            var productTaxMap = taxSetting.getTaxes().stream()
                    .filter(taxValue -> NumberUtils.isPositive(taxValue.getProductId()))
                    .collect(Collectors.toMap(TaxSettingValue::getProductId, Function.identity(), (first, second) -> second));
            productInfoBuilder
                    .countryTax(countryTax)
                    .productTaxMap(productTaxMap);
        }

        return productInfoBuilder.build();
    }

    @Override
    public void validate(CombinationCalculateRequest request) {
        if (CollectionUtils.isEmpty(request.getLineItems())) {
            if (log.isDebugEnabled()) {
                log.debug("Line item is empty");
            }
            return;
        }

        List<UserError> userErrors = new ArrayList<>();
        for (int i = 0; i < request.getLineItems().size(); i++) {
            var lineItemRequest = request.getLineItems().get(i);
            if (lineItemRequest.isCustom() && (lineItemRequest.getPrice() == null || lineItemRequest.getPrice().signum() < 0)) {
                userErrors.add(UserError.builder()
                        .code("invalid")
                        .message("line_items[%s].%s".formatted(i, "price"))
                        .fields(List.of("price"))
                        .build());
            }
            if (CollectionUtils.isNotEmpty(lineItemRequest.getComponents())
                    && lineItemRequest.getType() == VariantType.normal) {
                userErrors.add(UserError.builder()
                        .code("line_items[%s].type".formatted(i))
                        .message("type must be combo or packsize")
                        .fields(List.of("line_items", "type"))
                        .build());
            }
        }
        if (CollectionUtils.isNotEmpty(userErrors)) {
            var errorMessageBuilder = ErrorMessage.builder();
            userErrors.forEach(errorMessageBuilder::addError);
            throw new ConstrainViolationException(errorMessageBuilder.build());
        }
    }
}
