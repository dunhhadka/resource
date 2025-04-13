package org.example.order.order.application.utils;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.example.order.order.infrastructure.configuration.exception.ConstrainViolationException;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public final class AddressHelper {

    private static final Pattern SPACES = Pattern.compile(" {2,}");

    public static Pair<String, String> breakIntoStructureName(String fullName) {
        String firstName = null;
        String lastName = null;
        if (StringUtils.isNotBlank(fullName)) {
            fullName = SPACES.matcher(fullName).replaceAll(" ").trim();
            int lastWhiteSpace = fullName.lastIndexOf(" ");
            if (lastWhiteSpace != -1) {
                firstName = fullName.substring(lastWhiteSpace + 1);
                lastName = fullName.substring(0, lastWhiteSpace);
            } else {
                firstName = fullName;
            }
        }
        return Pair.of(firstName, lastName);
    }

    /**
     * Khi mỗi module thực hiện 1 nhiệm vụ riêng biệt thì sẽ cô lập module đó
     * bằng cách, các module bên ngoài giao tiếp với module này phi map theo input/output của module này
     */
    public static AddressAreaModel resolve(AddressRequest request) {
        if (request == null) return new AddressAreaModel();

        Country country = null;
        Province province = null;
        District district = null;
        Ward ward = null;

        //ward
        var allWards = AddressUtils.wardList();
        ward = getWard(allWards, request);

        var allProvince = AddressUtils.provinceList();
        province = getProvince(allProvince, request, ward);

        return new AddressAreaModel(country, province, district, ward);
    }

    private static Province getProvince(List<Province> allProvince, AddressRequest request, Ward ward) {
        if (!NumberUtils.isPositive(request.getProvinceId())
                && StringUtils.isBlank(request.getProvinceCode())
                && StringUtils.isBlank(request.getProvince())
                && ward.getProvinceId() <= 0) {
            return null;
        }

        Province province = null;
        if (NumberUtils.isPositive(request.getProvinceId())) {
            province = findFirstById(allProvince, p -> Objects.equals(p.getId(), request.getProvinceId()));
        }
        if (province == null && StringUtils.isNotBlank(request.getProvinceCode())) {
            province = findFirstById(allProvince, p -> Objects.equals(p.getCode(), request.getProvinceCode()));
        }
        if (province == null && StringUtils.isNotBlank(request.getProvince())) {
            province = findFirstById(allProvince, p -> Objects.equals(p.getName(), request.getProvince()));
        }
        if (NumberUtils.isPositive(ward.getProvinceId())) {
            var provinceCheckedByWard = findFirstById(allProvince, p -> Objects.equals(p.getId(), ward.provinceId));
            if (province == null) {
                province = provinceCheckedByWard;
            } else if (provinceCheckedByWard.getId() != province.id) {
                throw new ConstrainViolationException(
                        "province",
                        "conflict province between ward and province"
                );
            }
        }

        return province;
    }

    private static Ward getWard(List<Ward> allWards, AddressRequest request) {
        if (!NumberUtils.isPositive(request.getWardId())
                && StringUtils.isBlank(request.getWardCode())
                && StringUtils.isBlank(request.getWard())) {
            return null;
        }
        Ward ward = null;
        if (NumberUtils.isPositive(request.getWardId())) {
            ward = findFirstById(allWards, w -> Objects.equals(w.getId(), request.getWardId()));
        }
        if (ward == null && StringUtils.isNotBlank(request.getWardCode())) {
            ward = findFirstById(allWards, w -> Objects.equals(w.getCode(), request.getWardCode()));
        }
        if (ward == null && StringUtils.isNotBlank(request.getWard())) {
            ward = findFirstById(allWards, w -> Objects.equals(w.getName(), request.getWard()));
        }
        return ward;
    }

    private static <T> T findFirstById(List<T> resources, Predicate<T> condition) {
        return resources.stream()
                .filter(condition)
                .findFirst()
                .orElse(null);
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddressAreaModel {
        private Country country;
        private Province province;
        private District district;
        private Ward ward;
    }

    @Getter
    @Setter
    @Builder
    public static class Province {
        private int id;
        private String name;
        private String code;
        @JsonProperty("country_id")
        private int countryId;
    }

    @Getter
    @Setter
    @Builder
    public static class District {
        private int id;
        private String name;
        private String code;
        @JsonProperty("province_id")
        private int provinceId;
    }

    @Getter
    @Setter
    @Builder
    public static class Ward {
        private int id;
        private String name;
        private String code;
        @JsonProperty("district_id")
        private int districtId;
        @JsonProperty("province_id")
        private int provinceId;
    }

    @Getter
    @Setter
    public static class Country {
        private int id;
        private String name;
        private String code;
    }

    @Getter
    @Setter
    @Builder(toBuilder = true)
    public static class AddressRequest {
        private Integer countryId;
        private String countryCode;
        private String country;
        private @Deprecated String countryName;

        private Integer provinceId;
        private String provinceCode;
        private String province;
        private String city;

        private Integer districtId;
        private String districtCode;
        private String district;

        private Integer wardId;
        private String wardCode;
        private String ward;
    }
}
