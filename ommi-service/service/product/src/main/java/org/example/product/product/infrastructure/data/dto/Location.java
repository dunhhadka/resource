package org.example.product.product.infrastructure.data.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Builder
public class Location {
    private int id;
    private int storeId;
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
    private String zip;
    private boolean fulfillOrder;
    private boolean inventoryManagement;
    private Instant deactivateInventoryAt;
    private boolean defaultLocation;
    private boolean offlineStore;
    private Instant startDate;
    private Instant endDate;
    private Instant createdOn;
    private Instant modifiedOn;
}
