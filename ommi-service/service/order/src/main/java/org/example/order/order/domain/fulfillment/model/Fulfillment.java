package org.example.order.order.domain.fulfillment.model;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.order.ddd.AggregateRoot;
import org.example.order.order.application.converter.ListNumberAttributeConverter;
import org.example.order.order.application.converter.StringMapConverter;
import org.example.order.order.domain.fulfillmentorder.model.FulfillmentOrder;
import org.example.order.order.domain.fulfillmentorder.model.FulfillmentOrderId;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Getter
@Entity
@Table(name = "fulfillments")
@NoArgsConstructor
public class Fulfillment extends AggregateRoot<Fulfillment> {

    @JsonUnwrapped
    @EmbeddedId
    @AttributeOverride(name = "storeId", column = @Column(name = "storeId"))
    @AttributeOverride(name = "id", column = @Column(name = "id"))
    private FulfillmentId id;

    @Size(max = 255)
    private String name;

    private Integer number;

    private int orderId;

    private Integer locationId;

    @Convert(converter = ListNumberAttributeConverter.class)
    private List<Integer> fulfillmentOrderIds;

    @CreationTimestamp
    private Instant createdOn;

    @UpdateTimestamp
    private Instant modifiedOn;

    private Instant cancelledOn;

    private Instant deliveredOn;

    private boolean notifyCustomer;

    @Enumerated(value = EnumType.STRING)
    private FulfillmentStatus status;

    @Enumerated(value = EnumType.STRING)
    private ShippingStatus shippingStatus;

    /**
     * Fulfillment line items: insert only, should never be updated
     */
    @OneToMany(mappedBy = "fulfillment", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    private List<@Valid FulfillmentLineItem> lineItems;

    @Valid
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "name", column = @Column(name = "originAddressName")),
            @AttributeOverride(name = "email", column = @Column(name = "originAddressEmail")),
            @AttributeOverride(name = "phone", column = @Column(name = "originAddressPhone")),
            @AttributeOverride(name = "address1", column = @Column(name = "originAddressAddress1")),
            @AttributeOverride(name = "address2", column = @Column(name = "originAddressAddress2")),
            @AttributeOverride(name = "ward", column = @Column(name = "originAddressWard")),
            @AttributeOverride(name = "wardCode", column = @Column(name = "originAddressWardCode")),
            @AttributeOverride(name = "district", column = @Column(name = "originAddressDistrict")),
            @AttributeOverride(name = "districtCode", column = @Column(name = "originAddressDistrictCode")),
            @AttributeOverride(name = "province", column = @Column(name = "originAddressProvince")),
            @AttributeOverride(name = "provinceCode", column = @Column(name = "originAddressProvinceCode")),
            @AttributeOverride(name = "city", column = @Column(name = "originAddressCity")),
            @AttributeOverride(name = "country", column = @Column(name = "originAddressCountry")),
            @AttributeOverride(name = "countryCode", column = @Column(name = "originAddressCountryCode")),
            @AttributeOverride(name = "zipCode", column = @Column(name = "originAddressZipCode"))
    })
    private OriginAddress originAddress;

    @Enumerated(value = EnumType.STRING)
    private DeliveryMethod deliveryMethod;

    @Convert(converter = StringMapConverter.class)
    private Map<@NotBlank @Size(max = 50) String, @Size(max = 255) String> receipt;

    @Version
    private Integer version;

    public Fulfillment(
            FulfillmentId id,
            int orderId,
            int locationId,
            FulfillmentOrderId fulfillmentOrderId,
            FulfillmentOrder.ExpectedDeliveryMethod deliveryMethod,
            boolean sendNotification,
            List<FulfillmentLineItem> fulfillmentLineItems,
            OriginAddress originAddress,
            ShippingStatus shippingStatus
    ) {
        this.id = id;
        this.orderId = orderId;
        this.locationId = locationId;
        this.fulfillmentOrderIds = List.of(fulfillmentOrderId.getId());
        this.deliveryMethod = DeliveryMethod.valueOf(deliveryMethod.name());
        this.notifyCustomer = sendNotification;
        this.status = FulfillmentStatus.success;
        this.shippingStatus = shippingStatus;
        this.originAddress = originAddress;
        this.setLineItems(fulfillmentLineItems);
    }

    private void setLineItems(List<FulfillmentLineItem> lineItems) {
        this.lineItems = lineItems;
        this.lineItems.forEach(line -> line.setFulfillment(this));
    }


    public enum FulfillmentStatus {
        pending, open, confirmed, success, cancelled, error, failure
    }

    public enum ShippingStatus {
        pending, delivering, delivered, returning, returned, cancelled, failed, retry_delivery, ready_to_pick, picked_up
    }

    public enum DeliveryMethod {
        // không vận chuyển
        none,
        // bán tại cửa hàng bán lẻ (offline)
        retail,
        // nhận tại cửa hàng (mua online, nhận offline)
        pick_up,
        // đối tác vc tích hợp
        external_service,
        // đối tác vc ngoài
        outside_shipper,
        // shipper ngoài
        external_shipper,
        // use "external_shipper" instead
        @Deprecated internal_shipper,
        // nhân viên cửa hàng
        employee,
        // sàn TMĐT
        ecommerce,
        //
        fulfillment_service,
        // -no longer used-
        @Deprecated shipping
    }
}
