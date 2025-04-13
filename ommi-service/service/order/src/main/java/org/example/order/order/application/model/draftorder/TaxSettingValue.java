package org.example.order.order.application.model.draftorder;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Builder
@Getter
public class TaxSettingValue {
    private BigDecimal rate;
    private String title;
    private Integer productId;
    /**
     * type == null: là cấu hình mặc định
     */
    private TaxType type;

    public enum TaxType {
        line_item,
        shipping
    }
}
