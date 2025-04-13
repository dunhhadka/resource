package org.example.order.order.infrastructure.data.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CurrencyDto {
    private int id;
    private String name;
    private String code;
    private String cultureCode;
    private String symbol;
    private String symbolPosition;
    private int currencyDecimalDigits;
}
