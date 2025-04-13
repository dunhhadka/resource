package org.example.order.order.application.service.orderedit;

import org.apache.commons.collections4.CollectionUtils;
import org.example.order.order.application.model.draftorder.TaxSetting;
import org.example.order.order.application.model.draftorder.TaxSettingValue;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TaxContext {

    private final TaxSetting taxSetting;

    private Map<Integer, TaxSettingValue> taxSettingValueMap;

    public TaxContext() {
        this.taxSetting = null;
    }

    public TaxContext(TaxSetting taxSetting) {
        this.taxSetting = taxSetting;
    }

    public TaxSettingValue getAppyTaxRateFor(Integer productId) {
        if (this.taxSettingValueMap == null) {
            this.initTaxValue();
        }

        return this.taxSettingValueMap.get(productId);
    }

    private void initTaxValue() {
        if (this.taxSetting == null) {
            this.taxSettingValueMap = new HashMap<>();
            return;
        }
        this.taxSettingValueMap = this.taxSetting.getTaxes().stream()
                .collect(Collectors.toMap(TaxSettingValue::getProductId, Function.identity()));
    }

    public boolean isTaxIncluded() {
        assert this.taxSetting != null;
        return this.taxSetting.isTaxIncluded();
    }

    public boolean isCalculateTax() {
        return this.taxSetting != null && CollectionUtils.isNotEmpty(this.taxSetting.getTaxes());
    }
}
