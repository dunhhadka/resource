package org.example.order.order.application.model.draftorder.response;

import lombok.Builder;
import lombok.Getter;
import org.example.order.order.application.model.draftorder.TaxSettingValue;
import org.example.order.order.application.service.draftorder.Combo;
import org.example.order.order.application.service.draftorder.Packsize;
import org.example.order.order.application.service.draftorder.ProductResponse;
import org.example.order.order.application.service.draftorder.VariantResponse;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.HashMap;
import java.util.Map;

@Getter
@Builder
public class CalculateProductInfo {
    @Builder.Default
    private Map<Integer, ProductResponse.Product> productMap = new HashMap<>();

    @Builder.Default
    private Map<Integer, VariantResponse.Variant> variantMap = new HashMap<>();

    /**
     * key: variantId
     * value: sản phẩm combo tương ứng với variantId
     */
    @Builder.Default
    private Map<Integer, Combo> comboMap = new HashMap<>();

    @Builder.Default
    private Map<Integer, Packsize> packsizeMap = new HashMap<>();

    @Builder.Default
    private Map<Integer, TaxSettingValue> productTaxMap = new HashMap<>();

    private TaxSettingValue countryTax;

    @Builder.Default
    private Currency currency = Currency.getInstance("VND");

    private BigDecimal remainderUnit;
}
