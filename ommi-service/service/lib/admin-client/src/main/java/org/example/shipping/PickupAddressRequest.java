package org.example.shipping;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PickupAddressRequest {
    private Long locationId;
    private String name;
    private String phone;
    private String email;
    private String address1;
    private String address2;
    private String ward;
    private String wardCode;
    private String district;
    private String districtCode;
    private String province;
    private String provinceCode;
    private String city;
    private String country;
    private String countryCode;
    private String zipCode;
}
