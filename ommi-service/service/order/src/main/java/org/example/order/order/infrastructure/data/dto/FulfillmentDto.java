package org.example.order.order.infrastructure.data.dto;

import lombok.Getter;
import lombok.Setter;
import org.example.order.order.domain.fulfillment.model.Fulfillment;

import java.time.Instant;

@Getter
@Setter
public class FulfillmentDto {
    private int storeId;
    private int id;
    private int orderId;

    private String name;
    private Integer number;

    private Integer locationId;

    private Instant createdOn;
    private Instant modifiedOn;
    private Instant cancelledOn;
    private Instant deliveredOn;

    private boolean notifyCustomer;

    private Fulfillment.FulfillmentStatus status;
    private Fulfillment.ShippingStatus shippingStatus;
}
