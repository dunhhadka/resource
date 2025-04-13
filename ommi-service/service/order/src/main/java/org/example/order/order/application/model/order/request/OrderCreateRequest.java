package org.example.order.order.application.model.order.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.example.order.order.domain.fulfillment.model.Fulfillment;
import org.example.order.order.domain.order.model.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Builder
public class OrderCreateRequest {
    @JsonAlias("processed_on")
    @JsonProperty("processed_at")
    private Instant processedAt;

    private @Size(max = 32) String cartToken;
    private @Size(max = 32) String checkoutToken;
    private Double checkoutValue;

    private @Size(max = 50) String source;
    private @Size(max = 50) String sourceName;

    private @Valid ClientDetailRequest clientDetailRequest;

    private @Size(max = 50) String email;
    private @Size(max = 21) String phone;

    private boolean buyerAcceptMarketing;

    private @Valid CustomerRequest customer;

    private @Valid AddressRequest billingAddress;
    private @Valid AddressRequest shippingAddress;

    private @NotEmpty List<@Valid LineItemRequest> lineItems;

    private List<@Valid ShippingLineRequest> shippingLines;

    private @Min(0) BigDecimal totalDiscounts;

    private @Size(max = 10) List<@Valid DiscountCodeRequest> discountCodes;

    private @Size(max = 50) List<@Valid DiscountApplicationRequest> discountApplications;

    private Order.FinancialStatus financialStatus;

    private @Size(max = 3) String currency;

    private @Size(max = 250) String gateway;

    private @Size(max = 20) String processingMethod;

    private List<@Valid OrderTransactionCreateRequest> transactions;

    private @Min(0) int totalWeight;

    private @Size(max = 2000) String landingSite;
    private @Size(max = 2000) String landingSiteRef;
    private @Size(max = 2000) String referringSite;
    private @Size(max = 2000) String reference;

    private @Size(max = 255) String sourceIdentifier;
    private @Size(max = 255) String sourceUrl;

    private @Size(max = 2000) String note;

    private @Size(max = 50) List<@Size(max = 250) String> tags;

    private @Size(max = 100) List<@Valid CustomAttributeRequest> noteAttributes;

    @Builder.Default
    private boolean sendReceipt = true;

    @Builder.Default
    private boolean sendFulfillmentReceipt = true;

    @Builder.Default
    private boolean sendWebhooks = true;

    private @Size(max = 50) List<@Valid TaxLineRequest> taxLines;

    private boolean taxExempt;

    private boolean taxesIncluded;

    private Integer assigneeId;

    private Integer locationId;

    private Integer userId;

    private List<@Valid CombinationLineRequest> combinationLineRequests;

    private DraftOrderReference draftOrder;

    private List<@Valid FulfillmentRequest> fulfillments;

    @Getter
    @Setter
    public static class DraftOrderReference {
        private @Positive int id;
    }

    public boolean isFromDraftOrder() {
        return this.draftOrder != null;
    }

    @Getter
    @Builder
    public static class CombinationLineRequest {
        private @Min(1) int variantId;
        private @Min(1) int productId;

        private @Min(0) BigDecimal price;

        private @Positive BigDecimal quantity;

        private @Size(max = 320) String title;
        private @Size(max = 500) String variantTitle;

        private @Size(max = 255) String vendor;

        private @Size(max = 255) String sku;

        private @Size(max = 50) String unit;

        private @Size(max = 50) String itemUnit;

        private @NotNull CombinationLine.Type type;
    }

    @Getter
    @Setter
    @Builder
    public static class DiscountApplicationRequest {
        private @Size(max = 255) String code;
        private @Size(max = 250) String title;
        private @Size(max = 250) String description;

        private DiscountApplication.TargetType targetType;
        private DiscountApplication.ValueType valueType;
        private DiscountApplication.RuleType ruleType;

        private BigDecimal value;

        private int index;
    }

    @Getter
    @Setter
    @Builder
    public static class DiscountCodeRequest {
        private @Size(max = 255) String code;
        private @Min(0) BigDecimal amount;
        @Builder.Default
        private OrderDiscountCode.ValueType type = OrderDiscountCode.ValueType.fixed_amount;
        @Builder.Default
        private Boolean custom = true;
    }

    @Getter
    @Setter
    public static class ShippingLineRequest {
        private @NotBlank @Size(max = 150) String title;
        private @Size(max = 150) String code;
        private @Size(max = 50) String source;
        private @NotNull @Min(0) BigDecimal price;
        private @Size(max = 100) List<@Valid DiscountAllocationRequest> discountAllocations;
        private @Size(max = 100) List<@Valid TaxLineRequest> taxLines;
    }

    @Getter
    @Setter
    @Builder
    public static class LineItemRequest {
        private Integer variantId;

        private @Min(0) BigDecimal price;
        private @Min(0) BigDecimal totalDiscount;
        private @Positive int quantity;
        private @Min(0) int grams;
        private Boolean requireShipping;

        private @Size(max = 255) String discountCode;

        private @Size(max = 320) String title;
        private @Size(max = 500) String variantTitle;
        private @Size(max = 50) String sku;
        private @Size(max = 500) String vendor;
        private @Size(max = 50) String unit;

        private boolean giftCard;

        private Boolean taxable;

        private @Size(max = 100) List<@Valid CustomAttributeRequest> properties;

        private @Size(max = 100) List<@Valid DiscountAllocationRequest> discountAllocations;

        private @Size(max = 100) List<@Valid TaxLineRequest> taxLines;

        private Integer combinationLineIndex;
    }

    @Getter
    @Setter
    @Builder
    public static class TaxLineRequest {
        private @NotNull @Min(0) BigDecimal rate;

        @Builder.Default
        private String title = "Tax";

        @Builder.Default
        private BigDecimal price = BigDecimal.ZERO;

        public TaxLineRequest addPrice(BigDecimal price) {
            this.price = this.price.add(price);
            return this;
        }
    }

    @Getter
    @Setter
    @Builder(toBuilder = true)
    public static class DiscountAllocationRequest {
        private @Min(0) BigDecimal amount;

        private BigDecimal discountedAmount;

        private DiscountAllocation.TargetType targetType;

        private int targetId;

        private int discountApplicationIndex;
    }


    @Getter
    @Setter
    public static class AddressRequest {
        @Size(max = 50)
        private String firstName;
        @Size(max = 50)
        private String lastName;
        @Size(max = 100)
        private String name;

        @Size(max = 250)
        private String phone;
        @Size(max = 255)
        private String address1;
        @Size(max = 255)
        private String address2;
        @Size(max = 255)
        private String company;

        @Min(0)
        private Integer countryId;
        @Size(max = 10)
        private String countryCode;
        @Size(max = 50)
        private String country;
        @Size(max = 50)
        private String countryName;

        @Min(0)
        private Integer provinceId;
        @Size(max = 10)
        private String provinceCode;
        @Size(max = 50)
        private String province;
        @Size(max = 50)
        private String city;

        @Min(0)
        private Integer districtId;
        @Size(max = 30)
        private String districtCode;
        @Size(max = 50)
        private String district;

        @Min(0)
        private Integer wardId;
        @Size(max = 20)
        private String wardCode;
        @Size(max = 50)
        private String ward;

        @Size(max = 20)
        private String zip;
        @Size(max = 50)
        private String latitude;
        @Size(max = 50)
        private String longitude;
    }

    @Getter
    @Setter
    public static class CustomerRequest {
        private @Min(0) int id;
        private @Size(max = 50) String email;
        private @Size(max = 21) String phone;

        public boolean isEmpty() {
            return this.id == 0
                    && StringUtils.isBlank(this.email)
                    && StringUtils.isBlank(this.phone);
        }
    }

    @Getter
    @Setter
    public static class ClientDetailRequest {
        private @Size(max = 50) String browserIp;
        private @Size(max = 500) String acceptLanguage;
        private @Size(max = 1000) String userAgent;
        private String sessionHash;
        private int browserWight;
        private int browserHeight;
    }

    @Getter
    @Setter
    public static class FulfillmentRequest {
        @NotNull
        private Fulfillment.DeliveryMethod deliveryMethod;
        private Fulfillment.ShippingStatus shippingStatus;

        @Valid
        private ShippingInfoInit shippingInfo;

        private boolean sendNotification;

        @Valid
        private PickupAddress pickupAddress;

        @Valid
        private ShippingAddressInfo shippingAddress;

        @Size(max = 500)
        private String note;
    }

    @Getter
    @Setter
    private static class ShippingInfoInit {
        private BigDecimal serviceFee;
        private BigDecimal codAmount;
        private BigDecimal insuranceValue;
        @Size(max = 1000)
        private String metadata;
        private int width;
        private int height;
        private int length;
        private int weight;
        private int requirement;
    }

    @Getter
    @Setter
    private static class ShippingAddressInfo {
        @Size(max = 50)
        private String firstName;

        @Size(max = 50)
        private String lastName;

        @Size(max = 100)
        private String fullName;

        @Size(max = 128)
        private String email;

        @Size(max = 25)
        private String phone;

        @NotBlank
        @Size(max = 255)
        private String address1;

        @Size(max = 255)
        private String address2;

        @Size(max = 50)
        private String latitude;

        @Size(max = 50)
        private String longitude;

        @Size(max = 20)
        private String zipCode;
    }

    @Getter
    @Setter
    public static class PickupAddress {
        @NotNull
        private Long locationId;
        @Size(max = 128)
        private String name;
        @Size(max = 128)
        private String email;
        @Size(max = 25)
        private String phone;
        @NotBlank
        @Size(max = 255)
        private String address1;
        @Size(max = 255)
        private String address2;
        @Size(max = 20)
        private String zipCode;
    }
}
