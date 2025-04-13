package org.example.order.order.application.service.orderedit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.example.order.order.application.model.draftorder.TaxSetting;
import org.example.order.order.application.service.draftorder.TaxHelper;
import org.example.order.order.application.utils.OrderEditUtils;
import org.example.order.order.application.utils.TaxLineUtils;
import org.example.order.order.domain.order.model.LineItem;
import org.example.order.order.domain.order.model.Order;
import org.example.order.order.domain.order.model.TaxLine;
import org.example.order.order.domain.order.persistence.OrderIdGenerator;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaxService {

    private final TaxHelper taxHelper;
    private final OrderIdGenerator orderIdGenerator;

    public void applyTaxToEditItems(
            Order order,
            List<LineItem> newLineItems,
            List<Pair<LineItem, BigDecimal>> increasedItems
    ) {
        if (order.isTaxExempt() || (newLineItems.isEmpty() && increasedItems.isEmpty())) return;

        List<LineItem> taxableLineItems = Stream
                .concat(
                        newLineItems.stream(),
                        increasedItems.stream().map(Pair::getKey))
                .filter(LineItem::isTaxable)
                .toList();
        Set<Integer> taxableProductIds = getTaxableProductIds(taxableLineItems);

        if (taxableProductIds.isEmpty() && log.isDebugEnabled()) {
            log.debug("Skipping tax calculation for Order with id {}", order.getId());
        }

        int taxableItemCount = taxableLineItems.size();
        taxableProductIds.add(0);

        var taxSetting = this.resolveTaxSetting(order, taxableProductIds);
        if (taxSetting == null) {
            if (log.isDebugEnabled()) {
                log.debug("Found no Tax Settings for Order with id {} and product ids list: {}",
                        order.getId(), taxableProductIds);
            }
            return;
        }

        var currency = order.getMoneyInfo().getCurrency();
        var taxIncluded = taxSetting.isTaxIncluded();

        var taxLineIds = this.orderIdGenerator.generateTaxLineIds(taxableItemCount);

        var newTaxLines = Stream
                .concat(
                        taxLineStreamFromNewLineItems(newLineItems, taxLineIds, taxSetting, currency, taxIncluded),
                        taxLineStreamFromChangedLineItems(increasedItems, taxLineIds, taxSetting, currency, taxIncluded))
                .toList();

        if (!newLineItems.isEmpty()) {
            order.applyNewTaxes(newTaxLines);
        }
    }

    private Stream<TaxLine> taxLineStreamFromChangedLineItems(
            List<Pair<LineItem, BigDecimal>> increasedItems,
            Deque<Integer> taxLineIds,
            TaxSetting taxSetting,
            Currency currency,
            boolean taxIncluded
    ) {
        return increasedItems
                .stream()
                .map(Pair::getKey)
                .filter(LineItem::isTaxable)
                .map(lineItem -> createTaxLine(lineItem, taxSetting, currency, taxIncluded, taxLineIds));
    }

    private Stream<TaxLine> taxLineStreamFromNewLineItems(
            List<LineItem> newLineItems,
            Deque<Integer> taxLineIds,
            TaxSetting taxSetting,
            Currency currency,
            boolean taxIncluded
    ) {
        return newLineItems
                .stream()
                .filter(LineItem::isTaxable)
                .map(line -> createTaxLine(line, taxSetting, currency, taxIncluded, taxLineIds));
    }

    private TaxLine createTaxLine(
            LineItem line,
            TaxSetting taxSetting,
            Currency currency,
            boolean taxIncluded,
            Deque<Integer> taxLineIds
    ) {
        var taxRate = taxSetting.getApplicableRate(line.getVariantInfo().getProductId());
        var taxPrice = TaxLineUtils.calculateTaxPrice(
                taxRate.getRate(),
                line.getDiscountedTotal(),
                currency, taxIncluded);
        return new TaxLine(
                taxLineIds.removeFirst(),
                taxRate.getTitle(),
                taxPrice,
                taxRate.getRate(),
                line.getId(),
                TaxLine.TargetType.line_item,
                line.getQuantity()
        );
    }

    private TaxSetting resolveTaxSetting(Order order, Set<Integer> taxableProductIds) {
        int storeId = order.getId().getStoreId();

        String countryCode = OrderEditUtils.getCountryCode(order);

        TaxSetting taxSetting = this.taxHelper.getTaxSetting(storeId, countryCode, taxableProductIds, false);
        if (CollectionUtils.isEmpty(taxSetting.getTaxes())) return null;

        taxSetting = taxSetting.toBuilder().taxIncluded(order.isTaxIncluded()).build();

        return taxSetting;
    }

    private Set<Integer> getTaxableProductIds(List<LineItem> taxableLineItems) {
        return taxableLineItems.stream()
                .map(lineItem -> lineItem.getVariantInfo().getProductId())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }
}
