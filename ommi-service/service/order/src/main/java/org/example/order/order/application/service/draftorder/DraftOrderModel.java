package org.example.order.order.application.service.draftorder;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class DraftOrderModel {
    private int id;
    private int storeId;

    private Integer orderId;
    private Integer customerId;

    private String email;
    private String customerEmail;
    private String customerName;
    private String customerNameHandle;

    private int number;
    private String name;

    private BigDecimal subtotalPrice;
    private BigDecimal totalPrice;
    private String currency;
}
