package org.example.order.order.application.service.draftorder;

import lombok.Builder;
import lombok.Getter;
import org.example.order.order.application.model.draftorder.ProductTax;
import org.example.order.order.application.model.draftorder.ShippingTax;
import org.example.order.order.application.model.draftorder.TaxSetting;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class SapoClient {

    public TaxSetting taxSettingGet(int storeId) {
        return null;
    }

    public List<ProductTax> productTaxes(int storeId, Set<Integer> productIds) {
        return List.of();
    }

    public ShippingTax shippingTax(int storeId, ShippingTaxFilter shippingTaxFilter) {
        return null;
    }

    public ComboResponse comboFilter(int storeId, ComboFilter comboFilter) {
        return null;
    }

    public PacksizeResponse packsizeFilter(int storeId, List<Integer> variantIds) {
        return null;
    }

    public VariantResponse variantFilter(int storeId, List<Integer> variantIds) {
        return null;
    }

    public ProductResponse productFilter(int storeId, List<Integer> productIds) {
        return null;
    }

    @Builder
    @Getter
    public static class ShippingTaxFilter {
        private String countryCode;
    }
}
