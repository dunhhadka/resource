package org.example.order.order.domain.fulfillmentorder.model;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@Builder
public class Destination {
    @Size(max = 50)
    protected String firstName;
    @Size(max = 50)
    protected String lastName;
    @Size(max = 25)
    protected String phone;
    @Size(max = 128)
    private String email;
    @Size(max = 255)
    private String address1;
    @Size(max = 255)
    private String address2;
    @Size(max = 50)
    private String ward;
    @Size(max = 10)
    private String wardCode;
    @Size(max = 50)
    private String district;
    @Size(max = 10)
    private String districtCode;
    @Size(max = 50)
    private String province;
    @Size(max = 10)
    private String provinceCode;
    @Size(max = 50)
    private String city;
    @Size(max = 50)
    private String country;
    @Size(max = 10)
    private String countryCode;
    @Size(max = 50)
    private String latitude;
    @Size(max = 50)
    private String longitude;
    @Size(max = 20)
    private String zipCode;
}
