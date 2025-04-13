package org.example.order.order.domain.order.model;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.order.order.application.utils.AddressHelper;

@Getter
@Embeddable
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MailingAddress {
    @Size(max = 50)
    protected String firstName;
    @Size(max = 50)
    protected String lastName;
    // TODO: add "name" property
    //@Size(max = 110)
    //protected String name;

    // TODO: add phone validator
    //@vn.sapo.omni.common.validator.annotation.Phone
    @Size(max = 250)
    protected String phone;

    @Size(max = 255)
    protected String address1;
    @Size(max = 255)
    protected String address2;
    @Size(max = 255)
    protected String company;
    @Size(max = 50)
    protected String country;
    @Size(max = 50)
    protected String city;
    @Size(max = 50)
    protected String province;
    @Size(max = 50)
    protected String district;
    @Size(max = 50)
    protected String ward;

    @Size(max = 10)
    protected String countryCode;
    @Size(max = 10)
    protected String provinceCode;
    @Size(max = 30)
    protected String districtCode;
    @Size(max = 20)
    protected String wardCode;

    @Size(max = 20)
    protected String zip;
    @Size(max = 50)
    protected String latitude;
    @Size(max = 50)
    protected String longitude;

    @Deprecated
    @Size(max = 50)
    protected String countryName;

    public MailingAddress(
            String firstName,
            String lastName,
            String phone,
            String address1,
            String address2,
            String company,
            AddressHelper.Country country,
            AddressHelper.Province province,
            AddressHelper.District district,
            AddressHelper.Ward ward
    ) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.address1 = address1;
        this.address2 = address2;
        this.company = company;
        if (ward != null) {
            this.ward = ward.getName();
            this.wardCode = ward.getCode();
            if (district != null) {
                this.district = district.getName();
                this.districtCode = district.getCode();
            }
            if (province != null) {
                this.province = province.getName();
                this.provinceCode = province.getCode();
            }
            if (country != null) {
                this.country = country.getName();
                this.countryCode = country.getCode();
            }
        }
    }
}
