package org.example.order.order.application.service.orderedit;

import lombok.Getter;
import org.example.order.order.application.model.orderedit.response.CalculatedTaxLine;
import org.example.order.order.infrastructure.data.dao.RefundTaxLineDto;
import org.example.order.order.infrastructure.data.dao.TaxLineDto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;

public final class MergedTaxLine implements GenericTaxLine {

    private final TaxLineKey taxLineKey;

    private BigDecimal price = BigDecimal.ZERO;

    @Getter
    private BigDecimal quantity;
    private BigDecimal pricePerUnit;

    public MergedTaxLine(TaxLineKey taxLineKey) {
        this.taxLineKey = taxLineKey;
    }

    public static Collector<Map<TaxLineKey, MergedTaxLine>, Map<TaxLineKey, MergedTaxLine>, List<CalculatedTaxLine>> mergedMaps() {
        return Collector.of(
                HashMap::new,
                MergedTaxLine::mergeMap,
                MergedTaxLine::throwOnParallel,
                MergedTaxLine::toList
        );
    }

    private static List<CalculatedTaxLine> toList(Map<TaxLineKey, MergedTaxLine> mergedMap) {
        return mergedMap.values().stream()
                .map(CalculatedTaxLine::new)
                .toList();
    }

    private static <T> T throwOnParallel(T t1, T t2) {
        throw new UnsupportedOperationException(
                """
                        This collector doesn't support parallelization
                        """
        );
    }

    private static void mergeMap(Map<TaxLineKey, MergedTaxLine> merged, Map<TaxLineKey, MergedTaxLine> mergedInput) {
        mergedInput.forEach((taxLineKey, mergedTaxLine) -> {
            merged.merge(taxLineKey, mergedTaxLine, MergedTaxLine::merge);
        });
    }

    public static Collector<TaxLineDto, Map<TaxLineKey, MergedTaxLine>, Map<TaxLineKey, MergedTaxLine>> mergeToMap() {
        return Collector.of(
                HashMap::new,
                MergedTaxLine::mergeMap,
                MergedTaxLine::throwOnParallel
        );
    }

    private static void mergeMap(Map<TaxLineKey, MergedTaxLine> mergedTaxLine, TaxLineDto taxLineDto) {
        var taxLineKey = MergedTaxLine.TaxLineKey.from(taxLineDto);
        var mergeTaxLine = new MergedTaxLine(taxLineKey).merge(taxLineDto);
        mergedTaxLine.merge(taxLineKey, mergeTaxLine, MergedTaxLine::merge);
    }

    private MergedTaxLine merge(MergedTaxLine taxLine) {
        assert taxLine.getPrice() != null;

        this.addPrice(taxLine.getPrice());

        this.quantity = this.quantity.add(taxLine.getQuantity());

        return this;
    }

    public MergedTaxLine merge(TaxLineDto taxLine) {
        addPrice(taxLine.getPrice());

        this.quantity = this.quantity.add(BigDecimal.valueOf(taxLine.getQuantity()));

        return this;
    }

    private void addPrice(BigDecimal price) {
        if (price.signum() != 0) {
            this.price = this.price.add(price);
        }
    }

    public MergedTaxLine mergeRefunds(List<RefundTaxLineDto> refundTaxLines) {
        for (var refund : refundTaxLines) merge(refund);

        return this;
    }

    private void merge(RefundTaxLineDto refund) {
        addPrice(refund.getAmount().negate());
    }

    public void reduce(BigDecimal taxQuantity) {
        this.price = this.price.subtract(pricePerUnit().multiply(taxQuantity));
        this.quantity = this.quantity.subtract(taxQuantity);
    }

    public record TaxLineKey(
            String title,
            BigDecimal rate,
            boolean custom
    ) {
        public static TaxLineKey from(TaxLineDto taxLine) {
            return new TaxLineKey(taxLine.getTitle(), taxLine.getRate(), taxLine.isCustom());
        }
    }

    private BigDecimal pricePerUnit() {
        if (this.pricePerUnit == null) this.pricePerUnit = this.price.divide(this.quantity, RoundingMode.HALF_UP);
        return this.pricePerUnit;
    }

    @Override
    public String getTitle() {
        return this.taxLineKey.title;
    }

    @Override
    public BigDecimal getRate() {
        return this.taxLineKey.rate;
    }

    @Override
    public BigDecimal getPrice() {
        return this.price;
    }

    @Override
    public boolean isCustom() {
        return this.taxLineKey.custom;
    }
}
