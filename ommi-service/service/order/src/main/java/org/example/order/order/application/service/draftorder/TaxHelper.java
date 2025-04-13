package org.example.order.order.application.service.draftorder;

import org.example.order.order.application.model.draftorder.TaxSetting;

import java.util.Set;

public interface TaxHelper {
    TaxSetting getTaxSetting(int storeId, String countryCode, Set<Integer> productIds, boolean includeShipping);
}
