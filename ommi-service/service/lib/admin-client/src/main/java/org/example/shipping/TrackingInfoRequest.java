package org.example.shipping;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TrackingInfoRequest {
    private String carrier;
    private String carrierName;
    private String service;
    private String serviceName;
    private List<String> trackingNumbers;
    private List<String> trackingUrls;
    private String trackingNumber;
    private String trackingUrl;
    private String trackingReference;
    private String routeCode;
    private String referenceStatus;
    private String referenceStatusExplanation;
}
