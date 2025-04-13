package org.example.order.order.application.utils;

import org.apache.commons.collections4.CollectionUtils;
import org.example.order.order.application.model.combination.response.ComboPacksizeTaxLineResponse;
import org.example.order.order.application.model.draftorder.TaxSettingValue;
import org.example.order.order.domain.draftorder.model.DraftOrder;
import org.example.order.order.domain.draftorder.model.DraftOrderAddress;
import org.example.order.order.domain.draftorder.model.DraftTaxLine;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public final class TaxLineUtils {

    public static DraftTaxLine buildDraftTaxLine(TaxSettingValue taxSettingValue, BigDecimal price, Currency currency, boolean taxesIncluded) {
        return DraftTaxLine.builder()
                .title(taxSettingValue.getTitle())
                .rate(taxSettingValue.getRate())
                .ratePercentage(taxSettingValue.getRate().add(BigDecimals.ONE_HUND0RED))
                .price(calculateTaxPrice(taxSettingValue.getRate(), price, currency, taxesIncluded))
                .build();
    }

    public static BigDecimal calculateTaxPrice(BigDecimal rate, BigDecimal price, Currency currency, boolean taxesIncluded) {
        BigDecimal taxPrice = price.multiply(rate);
        if (taxesIncluded)
            taxPrice = taxPrice.divide(BigDecimal.ONE.add(rate), currency.getDefaultFractionDigits(), RoundingMode.HALF_UP);
        return taxPrice.setScale(currency.getDefaultFractionDigits(), RoundingMode.HALF_UP);
    }

    public static void mergeTaxLine(Map<String, DraftTaxLine> mergedTaxLines, DraftTaxLine taxLine) {
        String key = taxLine.getTitle() + "-" + taxLine.getRate();
        mergedTaxLines.compute(key, (k, oldValue) -> oldValue == null ? taxLine : oldValue.merge(taxLine));
    }

    public static String resolveCountryCode(DraftOrder draftOrder) {
        return Optional.ofNullable(draftOrder.getBillingAddress())
                .or(() -> Optional.ofNullable(draftOrder.getShippingAddress()))
                .map(DraftOrderAddress::getCountryCode)
                .orElse("VND");
    }

    public static Map<String, ComboPacksizeTaxLineResponse> merge(List<ComboPacksizeTaxLineResponse> taxLines) {
        Map<String, ComboPacksizeTaxLineResponse> taxLineMap = new HashMap<>();
        if (CollectionUtils.isEmpty(taxLines)) return taxLineMap;
        taxLines.forEach(taxLine -> {
            String key = taxLine.getTitle() + "-" + taxLine.getRate();
            taxLineMap.compute(key, (k, old) -> old == null ? taxLine : old.addPrice(taxLine.getPrice()));
        });
        return taxLineMap;
    }

    public static BigDecimal distribute(BigDecimal taxPrice, BigDecimal totalLinePrice, BigDecimal itemPrice, Currency currency) {
        if (totalLinePrice.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return itemPrice.multiply(taxPrice).divide(totalLinePrice, currency.getDefaultFractionDigits(), RoundingMode.HALF_UP);
    }

    public static ComboPacksizeTaxLineResponse buildTaxLine(
            TaxSettingValue productTax,
            TaxSettingValue countryTax,
            BigDecimal subtotal,
            Currency currency,
            boolean taxIncluded
    ) {
        if (productTax != null) {
            return buildTaxLine(productTax, subtotal, currency, taxIncluded);
        }
        return buildTaxLine(countryTax, subtotal, currency, taxIncluded);
    }

    private static ComboPacksizeTaxLineResponse buildTaxLine(TaxSettingValue productTax, BigDecimal subtotal, Currency currency, boolean taxIncluded) {
        var rate = productTax.getRate();
        return ComboPacksizeTaxLineResponse.builder()
                .rate(rate)
                .title(productTax.getTitle())
                .price(TaxLineUtils.calculateTaxPrice(rate, subtotal, currency, taxIncluded))
                .build();
    }

    public static Collector<ComboPacksizeTaxLineResponse, Map<String, ComboPacksizeTaxLineResponse>, List<ComboPacksizeTaxLineResponse>> merge() {
        return Collector.of(
                HashMap::new,
                TaxLineUtils::mergedTaxLine,
                (map1, map2) -> {
                    map2.forEach((key, value) -> map1.merge(key, value, (v1, v2) -> v1.addPrice(v2.getPrice())));
                    return map1;
                },
                map -> new ArrayList<>(map.values())
        );
    }

    private static void mergedTaxLine(Map<String, ComboPacksizeTaxLineResponse> merged, ComboPacksizeTaxLineResponse taxLine) {
        String taxLineKey = taxLine.getTitle() + "_" + taxLine.getRate();
        merged.merge(taxLineKey, taxLine, (oldValue, newValue) -> oldValue.addPrice(newValue.getPrice()));
    }
}
