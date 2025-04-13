package org.example.order.order.application.service.draftorder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.example.order.order.application.model.draftorder.ProductTax;
import org.example.order.order.application.model.draftorder.ShippingTax;
import org.example.order.order.application.model.draftorder.TaxSetting;
import org.example.order.order.application.model.draftorder.TaxSettingValue;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaxHelperImpl implements TaxHelper {

    private final SapoClient sapoClient;

    private static final int MAX_RETRY = 3;
    private static final int TIME_OUT = 5;
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    @Override
    public TaxSetting getTaxSetting(int storeId, String countryCode, Set<Integer> productIds, boolean includeShipping) {
        log.info("Fetch taxSetting for store: {}, countryCode: {}, productIds: {}", storeId, countryCode, productIds);

        CompletableFuture<TaxSetting> taxSettingSupply = fetchAsync(() -> sapoClient.taxSettingGet(storeId));
        CompletableFuture<List<ProductTax>> productTaxSupply = CollectionUtils.isEmpty(productIds)
                ? CompletableFuture.completedFuture(List.of())
                : fetchAsync(() -> sapoClient.productTaxes(storeId, productIds));
        CompletableFuture<ShippingTax> shippingTaxSupply = includeShipping
                ? fetchAsync(() -> sapoClient.shippingTax(storeId, SapoClient.ShippingTaxFilter.builder().countryCode(countryCode).build()))
                : CompletableFuture.completedFuture(null);

        CompletableFuture.allOf(taxSettingSupply, productTaxSupply, shippingTaxSupply)
                .exceptionally(err -> {
                    log.error("Error fetching tax data", err);
                    return null;
                })
                .join();

        TaxSetting taxSetting = taxSettingSupply.join();
        List<ProductTax> productTaxes = productTaxSupply.join();
        ShippingTax shippingTax = shippingTaxSupply.join();

        if (taxSetting != null && CollectionUtils.isNotEmpty(productTaxes) && (!includeShipping || shippingTax != null)) {
            return getTaxSetting(taxSetting, productTaxes, shippingTax, countryCode);
        }

        return TaxSetting.defaultTax();
    }

    private TaxSetting getTaxSetting(TaxSetting taxSetting, List<ProductTax> productTaxes, ShippingTax shippingTax, String countryCode) {
        List<TaxSettingValue> taxes = new ArrayList<>();
        for (var productTax : productTaxes) {
            var taxValueBuilder = TaxSettingValue.builder()
                    .title(productTax.getTaxName())
                    .rate(productTax.getTaxRate());
            if (productTax.getProductId() > 0) {
                taxValueBuilder
                        .productId(productTax.getProductId())
                        .type(TaxSettingValue.TaxType.line_item);
            }
            taxes.add(taxValueBuilder.build());
        }
        if (shippingTax != null) {
            taxes.add(TaxSettingValue.builder()
                    .title(shippingTax.getTaxName())
                    .rate(shippingTax.getTaxRate())
                    .type(TaxSettingValue.TaxType.shipping)
                    .build());
        }
        return TaxSetting.builder()
                .taxes(taxes)
                .taxIncluded(taxSetting.isTaxIncluded())
                .taxShipping(taxSetting.isTaxShipping())
                .countryCode(countryCode)
                .build();
    }

    private <T> CompletableFuture<T> fetchAsync(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(() -> retryWithTimeout(supplier, MAX_RETRY, TIME_OUT), executorService);
    }

    private <T> T retryWithTimeout(Supplier<T> supplier, int maxRetry, int timeOut) {
        for (int i = 0; i < maxRetry; i++) {
            try {
                return CompletableFuture.supplyAsync(supplier)
                        .get(timeOut, TimeUnit.MICROSECONDS);
            } catch (TimeoutException exception) {
                log.warn("Attempt {}/{} - Timeout occurred", i, timeOut);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }
}
