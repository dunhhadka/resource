package org.example.order.order.application.utils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public final class AddressUtils {

    private static final ObjectMapper mapper;

    static {
        mapper = JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
    }

    static <T> T readFile(String path, Class<T> clazz) {
        var loader = AddressUtils.class.getClassLoader();
        try (InputStream is = loader.getResourceAsStream(path)) {
            return mapper.readValue(is, clazz);
        } catch (IOException exception) {
            throw new IllegalArgumentException("can't read file: " + path);
        }
    }

    public static class ProvinceHolder {
        private static final List<AddressHelper.Province> provinces = List.of(readFile("address/province.json", AddressHelper.Province[].class));
    }

    public static List<AddressHelper.Province> provinceList() {
        return ProvinceHolder.provinces;
    }

    public static class WardHolder {
        private static final List<AddressHelper.Ward> wards = List.of(readFile("address/ward.json", AddressHelper.Ward[].class));
    }

    public static List<AddressHelper.Ward> wardList() {
        return WardHolder.wards;
    }

    //country
    public static class CountriesHolder {
        private static final List<AddressHelper.Country> countries = List.of(readFile("address/country.json", AddressHelper.Country[].class));
    }

    public static List<AddressHelper.Country> countryList() {
        return CountriesHolder.countries;
    }

    public static class DistinctHolder {
        private static final List<AddressHelper.District> districts = List.of(readFile("address/district.json", AddressHelper.District[].class));
    }

    public static List<AddressHelper.District> distintList() {
        return DistinctHolder.districts;
    }
}
