package org.example.shipping;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Builder
@Getter
public class ShippingRequest {
    private int orderId;
    private int fulfillmentOrderId;
    private int fulfillmentId;
    private int locationId;
    private String orderName;
    private String locationName;
    private String deliveryMethod;
    private String deliveryStatus;
    private PickupAddressRequest pickupAddress;
    private ShippingAddressRequest shippingAddress;
    private ShippingInfoRequest shippingInfo;
    private TrackingInfoRequest trackingInfo;
    private List<LineItemRequest> lineItems;
    private String email;
    private String note;
    private List<String> tags;
    private boolean sendNotification;

    @Getter
    @Builder
    public static class LineItemRequest {
        private Integer productId;
        private String title;
        private Integer variantId;
        private String variantTitle;
        private String name;
        private boolean giftCard;
        private String vendor;
        private String variantInventoryManagement;
        private String sku;
        private String image;
        private BigDecimal price;
        private BigDecimal discountedUnitPrice;
        private Integer quantity;
        private BigDecimal originalTotal;
        private BigDecimal discountedTotal;
        private int grams;
        private Integer orderLineItemId;
    }
}
