package org.example.order.order.application.service.order;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.example.customer.Customer;
import org.example.order.order.application.model.order.es.OrderEsModel;
import org.example.order.order.application.model.order.request.OrderCreateRequest;
import org.example.order.order.application.utils.AddressHelper;
import org.example.order.order.application.utils.CustomerPhoneUtils;
import org.example.order.order.application.utils.NumberUtils;
import org.example.order.order.domain.order.model.*;
import org.example.order.order.infrastructure.data.dto.FulfillmentDto;
import org.mapstruct.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Mapper(componentModel = "spring")
public abstract class OrderMapper {

    public abstract AddressHelper.AddressRequest toAddressRequest(OrderCreateRequest.AddressRequest address);

    @Mapping(source = "order.id.storeId", target = "storeId")
    @Mapping(source = "order.id.id", target = "orderId")
    @Mapping(source = "order.createdOn", target = "createdOn")
    @Mapping(source = "order.modifiedOn", target = "modifiedOn")
    @Mapping(source = "order.tags", target = "tags", qualifiedByName = "fromOrderTagToTagStrings")
    @Mapping(source = "orderFulfillments", target = "fulfillments", qualifiedByName = "toFulfillmentEsModels")
    public abstract OrderEsModel toEsModel(Order order, Customer customer, List<FulfillmentDto> orderFulfillments);

    @Named("toFulfillmentEsModels")
    protected List<OrderEsModel.FulfillmentEsModel> toFulfillmentEsModels(List<FulfillmentDto> orderFulfillments) {
        if (CollectionUtils.isEmpty(orderFulfillments)) {
            return List.of();
        }
        return orderFulfillments.stream()
                .map(this::toFulfillmentEsModel)
                .toList();
    }

    public abstract OrderEsModel.FulfillmentEsModel toFulfillmentEsModel(FulfillmentDto fulfillmentDto);

    @Named("fromOrderTagToTagStrings")
    protected List<String> fromOrderTagToTagStrings(List<OrderTag> orderTags) {
        if (CollectionUtils.isEmpty(orderTags)) return List.of();
        return orderTags.stream()
                .map(OrderTag::getName)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toList();
    }

    /**
     * Mapping thêm 1 số trường
     */
    @AfterMapping
    protected void afterMapToEsModel(@MappingTarget OrderEsModel orderEsModel, Order order, Customer customer, List<FulfillmentDto> fulfillments) {
        fillPhones(orderEsModel, order);

        var billingAddress = order.getBillingAddress();
        var shippingAddress = order.getShippingAddress();

        // custom thêm những option để search
        var otherTexts = new ArrayList<String>();
        otherTexts.addAll(getSearchTextsFromAddress(billingAddress));
        otherTexts.addAll(getSearchTextsFromAddress(shippingAddress));

        this.addDiscountCodes(otherTexts, order);

        this.addComboPacksizeItems(orderEsModel, order);

        this.mapProductInfos(orderEsModel, otherTexts);

        this.fillSearchTexts(otherTexts, orderEsModel);
    }

    private void fillSearchTexts(ArrayList<String> otherTexts, OrderEsModel orderEsModel) {
        if (CollectionUtils.isEmpty(otherTexts)) {
            return;
        }

        var texts = otherTexts.stream()
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toList();
        if (CollectionUtils.isNotEmpty(texts)) {
            orderEsModel.setSearchTexts(texts);
        }
    }

    private void mapProductInfos(OrderEsModel orderEsModel, List<String> otherTexts) {
        var variantIds = orderEsModel.getLineItems().stream()
                .map(OrderEsModel.LineItemEsModel::getVariantId)
                .filter(NumberUtils::isPositive)
                .map(String::valueOf)
                .distinct()
                .toList();
        var productNames = orderEsModel.getLineItems().stream()
                .map(OrderEsModel.LineItemEsModel::getName)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toList();
        var skus = orderEsModel.getLineItems().stream()
                .map(OrderEsModel.LineItemEsModel::getSku)
                .filter(StringUtils::isNotBlank)
                .distinct().toList();

        otherTexts.addAll(variantIds);
        otherTexts.addAll(productNames);
        otherTexts.addAll(skus);
    }

    private void addComboPacksizeItems(OrderEsModel orderEsModel, Order order) {
        if (CollectionUtils.isNotEmpty(order.getCombinationLines())) {
            return;
        }

        var combinationEsLines = order.getCombinationLines().stream()
                .map(this::toLineItemEsModel)
                .toList();
        orderEsModel.addLineItems(combinationEsLines);
    }

    protected abstract OrderEsModel.LineItemEsModel toLineItemEsModel(CombinationLine line);

    private void addDiscountCodes(ArrayList<String> otherTexts, Order order) {
        if (CollectionUtils.isEmpty(order.getDiscountCodes())) {
            return;
        }

        var codes = order.getDiscountCodes().stream()
                .map(OrderDiscountCode::getCode)
                .filter(StringUtils::isNotBlank)
                .distinct().toList();
        if (CollectionUtils.isNotEmpty(codes)) {
            otherTexts.addAll(codes);
        }
    }

    private List<String> getSearchTextsFromAddress(OrderAddress address) {
        if (address == null) return List.of();

        var addressDetail = address.getAddress();
        return Stream.of(
                getFullName(address),
                addressDetail.getCompany()
        ).filter(StringUtils::isNotBlank).toList();
    }

    private String getFullName(OrderAddress address) {
        if (address.getAddress() == null) return StringUtils.EMPTY;

        var addressDetail = address.getAddress();
        if (StringUtils.isAllBlank(addressDetail.getFirstName(), addressDetail.getLastName())) {
            return StringUtils.EMPTY;
        }
        return AddressHelper.getFullName(addressDetail.getFirstName(), addressDetail.getLastName());
    }

    private void fillPhones(OrderEsModel orderEsModel, Order order) {
        var billing = order.getBillingAddress();
        var shipping = order.getShippingAddress();

        var result = new ArrayList<String>();
        if (billing != null) {
            result.addAll(handleEsPhone(billing.getAddress().getPhone()));
        }
        if (shipping != null) {
            result.addAll(handleEsPhone(shipping.getAddress().getPhone()));
        }
        var phones = result.stream().filter(StringUtils::isNotBlank).distinct().toList();
        orderEsModel.setPhones(phones);
    }

    private List<String> handleEsPhone(String phone) {
        var phones = new ArrayList<String>();
        if (StringUtils.isNotBlank(phone)) {
            var response = CustomerPhoneUtils.normalize(phone);
            phones.add(response);
            if (response.startsWith("+84")) phones.add(String.format("0%s", response.substring(3)));
        }
        return phones;
    }
}