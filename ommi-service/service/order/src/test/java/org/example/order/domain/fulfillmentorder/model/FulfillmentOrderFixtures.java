package org.example.order.domain.fulfillmentorder.model;

import org.example.order.infrastructure.InMemoryIdGenerator;
import org.example.order.order.domain.fulfillmentorder.model.*;
import org.example.order.order.domain.fulfillmentorder.persistence.FulfillmentOrderIdGenerator;

import java.time.Instant;

public interface FulfillmentOrderFixtures {

    FulfillmentOrderIdGenerator idGenerator = new InMemoryIdGenerator();

    int storeId = 1;
    int orderId = 1;

    FulfillmentOrderId fulfillmentOrderId = new FulfillmentOrderId(storeId, idGenerator.generateFulfillmentOrderId());

    Long assignedLocationId = 123L;

    boolean requireShipping = true;

    FulfillmentOrder.ExpectedDeliveryMethod expectedDeliveryMethod = FulfillmentOrder.ExpectedDeliveryMethod.external_service;

    AssignedLocation assignedLocation = AssignedLocation.builder()
            .address1("266 Đội Cấn")
            .city("Ha noi")
            .name("Kho Đội Cấn")
            .phone("0906907234")
            .province("Hà Nội")
            .provinceCode("1")
            .country("Vietnam")
            .countryCode("VN")
            .district("Quận Ba Đình")
            .districtCode("2")
            .ward("Phường Cống vị")
            .wardCode("4")
            .build();

    Destination destination = Destination.builder()
            .address1("Lầu 5, Tòa Lữ Gia, Số 70 Lữ Gia")
            .firstName("John")
            .lastName("Doe")
            .phone("0982345678")
            .province("TP Hồ Chí Minh")
            .provinceCode("2")
            .country("Vietnam")
            .countryCode("VN")
            .district("Quận 11")
            .districtCode("40")
            .ward("Phường 15")
            .wardCode("9380")
            .build();

    FulfillmentOrderLineItem lineItem1 = null;

    default FulfillmentOrder partialFulfillmentOrder() {
        var fulfillmentOrder = new FulfillmentOrder(
                fulfillmentOrderId,
                assignedLocationId, expectedDeliveryMethod,
                requireShipping, assignedLocation, destination,
                Instant.now(),
                idGenerator);

        return null;
    }
}
