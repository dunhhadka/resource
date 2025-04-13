package org.example.order.domain.order.model;

import org.example.order.infrastructure.InMemoryIdGenerator;
import org.example.order.order.domain.order.model.*;
import org.example.order.order.domain.order.persistence.OrderIdGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.List;

public interface OrderFixtures {

    OrderIdGenerator idGenerator = new InMemoryIdGenerator();

    default Order defaultOrder() {
        return new Order(
                storeId, processedAt, customerInfo,
                trackingInfo, currency, gateWay, processingMethod, totalWeight,
                note, tags, null, billingAddress, shippingAddress,
                lineItems(), shippingLines, discountCodes,
                discountApplications, discountAllocations,
                idGenerator, taxExempt, false, null, locationId, List.of()
        );
    }

    int storeId = 1;

    int locationId = 123;

    Instant processedAt = Instant.now();

    CustomerInfo customerInfo = CustomerInfo.builder()
            .email("shop@gmail.com")
            .phone("0836023098")
            .build();

    TracingInfo trackingInfo = TracingInfo.builder().build();

    Currency currency = Currency.getInstance("VND");

    List<String> tags = List.of("tag1", "tag2");

    String note = "note";

    String processingMethod = "manual";

    String gateWay = "banking";

    int totalWeight = 100;

    BillingAddress billingAddress = BillingAddress.builder()
            .address(MailingAddress.builder()
                    .address1("Thanh Hoá, Triệu Sơn, Thọ Ngọc")
                    .firstName("Dũng")
                    .lastName("Hà")
                    .build())
            .build();

    ShippingAddress shippingAddress = ShippingAddress.builder()
            .address(MailingAddress.builder()
                    .address1("Thanh Hoá, Triệu Sơn, Thọ Ngọc")
                    .firstName("Dũng")
                    .lastName("Hà")
                    .build())
            .build();


    List<ShippingLine> shippingLines = List.of();

    int lineItemId1 = 123;
    int lineQuantity1 = 15;
    BigDecimal price1 = BigDecimal.valueOf(15_000);
    Integer variantId1 = 5;
    Integer productId1 = 55;


    int lineItemId2 = 234;
    int lineQuantity2 = 10;
    BigDecimal price2 = BigDecimal.valueOf(59_000);
    Integer variantId2 = 6;
    Integer productId2 = 45;

    int lineItemId3 = 345; // custom line
    int lineQuantity3 = 11;
    BigDecimal price3 = BigDecimal.valueOf(15_000);

    int lineItemId4 = 456;
    int lineQuantity4 = 15;
    BigDecimal price4 = BigDecimal.valueOf(115_000);
    Integer variantId4 = 5;
    Integer productId4 = 55;


    default List<LineItem> lineItems() {
        return List.of(
                new LineItem(lineItemId1, lineQuantity1, price1,
                        VariantInfo.builder().variantId(variantId1).productId(productId1).title("title1").build(),
                        List.of(), List.of(), false, null, null, null
                ),
                new LineItem(lineItemId2, lineQuantity2, price2,
                        VariantInfo.builder().variantId(variantId2).productId(productId2).title("title2").build(),
                        List.of(), List.of(), false, null, null, null
                ),
                new LineItem(lineItemId3, lineQuantity3, price3,
                        VariantInfo.builder().title("title1").build(),
                        List.of(), List.of(), false, null, null, null
                ),
                new LineItem(lineItemId4, lineQuantity4, price4,
                        VariantInfo.builder().variantId(variantId4).productId(productId4).title("title4").build(),
                        List.of(), List.of(), false, null, null, null
                )
        );
    }

    List<OrderDiscountCode> discountCodes = List.of();
    List<DiscountApplication> discountApplications = List.of();
    List<DiscountAllocation> discountAllocations = List.of();

    boolean taxExempt = false;
}
