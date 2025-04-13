package org.example.order.order.application.model.orderedit.response;

import lombok.Getter;
import lombok.Setter;
import org.example.order.order.domain.order.model.DiscountApplication;
import org.example.order.order.infrastructure.data.dao.OrderEditDiscountApplicationDto;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class CalculatedDiscountApplication {
    private UUID id;

    private String code;
    private String description;

    private BigDecimal value;
    private DiscountApplication.ValueType valueType;

    public CalculatedDiscountApplication(OrderEditDiscountApplicationDto application) {
        this.id = application.getId();

        this.code = application.getDescription();
        this.description = application.getDescription();

        this.value = application.getValue();
        this.valueType = application.getValueType();
    }
}
