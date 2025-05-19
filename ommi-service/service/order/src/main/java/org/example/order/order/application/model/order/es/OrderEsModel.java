package org.example.order.order.application.model.order.es;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
public class OrderEsModel {
    private int orderId;
    private int storeId;

    private int number;
    private int orderNumber;
    private String name;
    private String token;

    private Instant createdOn;
    private Instant modifiedOn;
    private Instant processedOn;
    private Instant closedOn;
    private Instant cancelledOn;

    private BigDecimal totalPrice;

    private List<String> paymentGatewayNames;
    private String gateway;
    private String processingMethod;

    private String source;
    private String sourceName;

    private String email;
    private String phone;
    private Integer customerId;
    private CustomerEsModel customer;

    private String status;
    private String fulfillmentStatus;
    private String financialStatus;
    private String cancelReason;

    private List<String> phones;
    private List<String> tags;

    private Integer locationId;
    private Integer userId;
    private Instant assignedId;

    private List<LineItemEsModel> lineItems;
    private List<ShippingLineEsModel> shippingLines;

    private List<FulfillmentEsModel> fulfillments;

    private AddressEsModel billingAddress;
    private AddressEsModel shippingAddress;

    private List<String> searchText;
    private List<String> searchTextNoSign;

    private List<String> orderPhones;

    private List<DiscountEsModel> discountCodes;

    private int printCount;

    private String currency;
    private BigDecimal unpaidAmount;

    private List<String> searchTexts;

    public OrderEsModel(int orderId, int storeId) {
        this.orderId = orderId;
        this.storeId = storeId;
    }

    public void addLineItems(List<LineItemEsModel> lineItems) {
        if (CollectionUtils.isEmpty(lineItems)) return;
        if (this.lineItems == null) {
            this.lineItems = lineItems;
            return;
        }
        this.lineItems.addAll(lineItems);
    }

    @Getter
    @Setter
    public static class CustomerEsModel {
        private int id;
        private String email;
        private String phone;
        private String name;
    }

    @Getter
    @Setter
    public static class LineItemEsModel {
        private Integer productId;
        private Integer variantId;
        private String name;
        private String sku;
        private String vendor;
    }

    @Getter
    @Setter
    public static class ShippingLineEsModel {
        private String title;
    }

    @Getter
    @Setter
    public static class FulfillmentEsModel {
        private String trackingNumber;
        private String trackingUrl;
        private String deliveryMethod;
        private String carrier;
        private String carrierName;
        private String shipmentStatus;
    }

    @Getter
    @Setter
    public static class AddressEsModel {
        private String countryCode;
        private String provinceCode;
        private String districtCode;
        private String wardCode;
        private String zip;
        private String company;
        private String provinceDistrict;
    }

    @Getter
    @Setter
    public static class DiscountEsModel {
        private String code;
    }
}
