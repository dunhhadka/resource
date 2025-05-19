package org.example.order.order.application.model.order.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
public class OrderSearchRequest extends PagingRequest {

    private String query;

    @JsonProperty("sort_by")
    private String sort;

    private String fieldsInclude;
    private String fieldsExclude;
    private List<Integer> ids;
    private String status;
    private List<String> statuses;
    private String financialStatus;
    private List<String> financialStatuses;
    private String fulfillmentStatus;
    private List<String> fulfillmentStatuses;

    private String tag;
    private List<String> tags;

    private int customerId;
    private List<Integer> customerIds;

    private int locationId;
    private List<Integer> locationIds;

    private Instant createdOnMin;
    private Instant createdOnMax;
    private Instant modifiedOnMin;
    private Instant modifiedOnMax;
    private Instant processedOnMin;
    private Instant processedOnMax;
    private Instant closedOnMin;
    private Instant closedOnMax;
    private Instant cancelledOnMin;
    private Instant cancelledOnMax;

    private int productId;
    private List<Integer> productIds;
    private int variantId;
    private List<Integer> variantIds;
    private List<Integer> userIds;

    private String currency;
    private BigDecimal unpaidAmountMin;
    private List<String> printStatuses;
}
