package org.example.order.order.application.service.order;

import org.example.order.order.application.model.order.request.OrderCreateRequest;
import org.example.order.order.application.utils.AddressHelper;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class OrderMapper {

    public abstract AddressHelper.AddressRequest toAddressRequest(OrderCreateRequest.AddressRequest address);
}
