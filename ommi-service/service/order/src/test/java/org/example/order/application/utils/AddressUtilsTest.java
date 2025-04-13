package org.example.order.application.utils;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AddressUtilsTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "123",
            "4567",
            "-123",
            "12.3",
            "abc"
    })
    void testSoNguyenDuong(String input) {
        Pattern pattern = Pattern.compile("^\\d+$");
        Matcher matcher = pattern.matcher(input);
        if (matcher.matches()) {
            System.out.println("Hợp lên");
        } else {
            System.out.println("Không hợp lệ");
        }
    }
}
