package org.example.order.order.application.service.combination;

import org.example.order.order.application.model.combination.request.CombinationCalculateRequest;
import org.example.order.order.application.model.combination.response.CombinationCalculateResponse;
import org.example.order.order.application.model.draftorder.response.CalculateProductInfo;

public abstract class CombinationProcessor {

    public CombinationCalculateResponse calculate(int storeId, CombinationCalculateRequest request) {
        validate(request);

        var productInfo = getProductInfo(storeId, request);

        CombinationCalculateResponse result;
        if (request.isUpdateProductInfo()) {
            result = calculate(request, productInfo);
        } else {
            result = calculateWithoutUpdateProduct(request, productInfo);
        }
        return result;
    }

    protected abstract CombinationCalculateResponse calculateWithoutUpdateProduct(CombinationCalculateRequest request, CalculateProductInfo productInfo);

    protected abstract CombinationCalculateResponse calculate(CombinationCalculateRequest request, CalculateProductInfo productInfo);

    protected abstract CalculateProductInfo getProductInfo(int storeId, CombinationCalculateRequest request);

    public abstract void validate(CombinationCalculateRequest request);
}
