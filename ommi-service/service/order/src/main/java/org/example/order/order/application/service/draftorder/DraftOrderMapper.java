package org.example.order.order.application.service.draftorder;

import org.example.order.order.application.model.combination.request.CombinationLineItemRequest;
import org.example.order.order.domain.draftorder.model.DraftLineItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public abstract class DraftOrderMapper {

    public abstract List<CombinationLineItemRequest> toCombinationLineItemRequests(List<DraftLineItem> lineItems);

//    @Mapping(target = "variantId", source = "lineItem.productInfo.variantId")
    public abstract CombinationLineItemRequest toCombinationLineItemRequest(DraftLineItem lineItem);
}
