package org.example.order.order.application.common;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.order.application.utils.JsonUtils;
import org.example.order.order.infrastructure.data.dto.CurrencyDto;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Currency;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SupportedCurrencies {

    private static class Holder {
        private static final Map<String, CurrencyDto> _currencies;

        static {
            String fileName = "json/currencies.json";
            var loader = SupportedCurrencies.class.getClassLoader();
            try (InputStream is = loader.getResourceAsStream(fileName)) {
                _currencies = Arrays.stream(JsonUtils.unmarshal(is, CurrencyDto[].class))
                        .collect(Collectors.toMap(CurrencyDto::getCode, Function.identity()));
            } catch (IOException e) {
                throw new IllegalStateException("can't read file: " + fileName);
            }
        }
    }


    public static CurrencyDto getCurrency(String currencyCode) {
        return Holder._currencies.get(currencyCode);
    }
}
