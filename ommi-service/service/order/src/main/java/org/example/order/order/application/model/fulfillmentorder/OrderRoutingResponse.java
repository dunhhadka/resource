package org.example.order.order.application.model.fulfillmentorder;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class OrderRoutingResponse {

    private List<OrderRoutingResult> results;

    @Builder
    @Getter
    public static class OrderRoutingResult {
        private OrderRoutingLocation location;
        private List<IndexesItem> indexesItems;
    }

    @Getter
    @Builder
    public static class IndexesItem {
        private int index;
        private Integer variantId;
        private String name;
        private Integer inventoryItemId;
    }

    @Builder
    @Getter
    public static class OrderRoutingLocation {
        private Long id;
        private Integer storeId;
        private String code;
        private String name;
        private String email;
        private String phone;
        private String country;
        private String countryCode;
        private String province;
        private String provinceCode;
        private String district;
        private String districtCode;
        private String ward;
        private String wardCode;
        private String address1;
        private String address2;
        private String zipCode;
    }
}
