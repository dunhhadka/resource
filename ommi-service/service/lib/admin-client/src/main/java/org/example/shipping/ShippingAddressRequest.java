package org.example.shipping;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShippingAddressRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String address1;
    private String address2;
    private String ward;
    private String district;
    private String province;
    private String country;
    private String city;
    private String countryCode;
    private String provinceCode;
    private String districtCode;
    private String wardCode;
    private String latitude;
    private String longitude;
    private String zipCode;
}
