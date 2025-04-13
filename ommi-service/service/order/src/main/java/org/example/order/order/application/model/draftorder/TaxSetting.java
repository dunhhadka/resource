package org.example.order.order.application.model.draftorder;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class TaxSetting {
    /**
     * taxIncluded = true: Giá hiển thị là giá đã tính cả thuế
     * taxIncluded = false: Giá hiển thị chưa tính thuế => sẽ tính thuế sau đó gộp vào giá đơn hàng
     */
    private boolean taxIncluded;

    private boolean taxShipping;

    private String countryCode;

    @Builder.Default
    private TaxStatus status = TaxStatus.inactive;

    private Set<Integer> productIds;

    private List<TaxSettingValue> taxes;


    public static TaxSetting defaultTax() {
        return new TaxSetting();
    }

    public TaxSettingValue getApplicableRate(Integer productId) {
        var taxMap = this.taxes.stream()
                .collect(Collectors.toMap(TaxSettingValue::getProductId, Function.identity()));

        if (productId == null) return taxMap.get(0);
        return taxMap.get(productId);
    }

    public enum TaxStatus {
        inactive,
        active
    }
}
