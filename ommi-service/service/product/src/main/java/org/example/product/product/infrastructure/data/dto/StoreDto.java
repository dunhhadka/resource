package org.example.product.product.infrastructure.data.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Builder
public class StoreDto {
    private int id;

    private String alias;

    private String name;

    private String metaTitle;

    private String metaDescription;

    private int activeThemeId;

    private Integer activeMobileThemeId;

    private String email;

    private String customerEmail;

    private int storePackageId;

    private Instant startDate;

    private Instant endDate;

    private Instant createdOn;

    private Instant modifiedOn;

    private String tradeName;

    private String phoneNumber;

    private String address;

    private String province;

    private Integer provinceId;

    private String country;

    private String countryCode;

    private String currency;

    private String moneyFormat;

    private String moneyWithCurrencyFormat;

    private String timezone;

    private int status;

    private boolean underConstructionMode;

    private boolean deleted;

    private boolean hasStorefront;

    private String provinceCode;

    private String source;

    private String storeOwner;

    private String planName;

    private String planDisplayName;

    private String domain;

    private BigDecimal usedVolumn;
    private BigDecimal maxVolumn;
    private int maxProduct;
}
