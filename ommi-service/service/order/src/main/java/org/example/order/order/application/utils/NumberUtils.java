package org.example.order.order.application.utils;

import java.math.BigDecimal;

public final class NumberUtils {

    public static boolean isPositive(Integer number) {
        return number != null && number > 0;
    }

    public static boolean isPositive(Long number) {
        return number != null && number > 0;
    }

    public static boolean isPositive(BigDecimal number) {
        return number != null && number.signum() > 0;
    }
}
