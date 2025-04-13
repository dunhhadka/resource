package org.example.shipping;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class ShippingInfoRequest {
    private BigDecimal serviceFee;
    private BigDecimal codAmount;
    private BigDecimal insuranceValue;
    private String freightPayer;
    private String paymentStatus;
    private String metadata;
    private int width;
    private int height;
    private int length;
    private int weight;
    private int requirement;
    private String note;
    private String deliveryStatus;
    @Builder.Default
    private String refundType = "no_restock";

    @Builder
    @Getter
    public static class Metadata {
        private String note;
        private String locationCurrently;
    }
}
