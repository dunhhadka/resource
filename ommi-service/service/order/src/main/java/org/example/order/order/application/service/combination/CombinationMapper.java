package org.example.order.order.application.service.combination;

import org.example.order.order.application.model.combination.request.CombinationLineItemRequest;
import org.example.order.order.application.model.combination.request.LineItemComponent;
import org.example.order.order.application.model.combination.response.CombinationLineItemComponent;
import org.example.order.order.application.model.combination.response.CombinationLineItemResponse;
import org.example.order.order.application.service.draftorder.ComboItem;
import org.example.order.order.application.service.draftorder.Packsize;
import org.example.order.order.application.service.draftorder.ProductResponse;
import org.example.order.order.application.service.draftorder.VariantResponse;
import org.example.order.order.domain.draftorder.model.VariantType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public abstract class CombinationMapper {

    public abstract VariantResponse.Variant toVariant(ComboItem item);

    public abstract CombinationLineItemResponse toLineItemResponse(CombinationLineItemRequest lineItemRequest);

    @Mapping(target = "title", source = "lineItem.title")
    @Mapping(target = "variantId", source = "variant.id")
    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "type", source = "variant.type")
    @Mapping(target = "unit", source = "variant.unit")
    @Mapping(target = "sku", source = "variant.sku")
    @Mapping(target = "taxable", source = "variant.taxable")
    @Mapping(target = "grams", source = "variant.grams")
    @Mapping(target = "price", source = "lineItem.price")
    @Mapping(target = "linePrice", source = "lineItem.linePrice")
    @Mapping(target = "variantTitle", source = "lineItem.title")
    @Mapping(target = "requireShipping", source = "variant.requiresShipping")
    public abstract CombinationLineItemResponse toLineItemResponse(CombinationLineItemRequest lineItem, VariantResponse.Variant variant, ProductResponse.Product product);

    public CombinationLineItemResponse toResponse(CombinationLineItemRequest lineItemRequest, VariantResponse.Variant variant, ProductResponse.Product product) {
        var lineItemResponse = this.toLineItemResponse(lineItemRequest, variant, product);
        if (lineItemResponse != null) {
            if (variant != null) {
                if (lineItemResponse.getVariantTitle() == null) lineItemResponse.setVariantTitle(variant.getTitle());
                if (lineItemResponse.getPrice() == null) lineItemResponse.setPrice(variant.getPrice());
                if (lineItemResponse.getSku() == null) lineItemResponse.setSku(variant.getSku());
            }
            if (product != null) {
                if (lineItemResponse.getTitle() == null) lineItemResponse.setTitle(product.getName());
            }
        }
        return lineItemResponse;
    }

    public abstract LineItemComponent toLineItemComponentResponse(Packsize packsize);

    @Mapping(target = "variantId", source = "variant.id")
    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "inventoryItemId", source = "variant.inventoryItemId")
    @Mapping(target = "sku", source = "variant.sku")
    @Mapping(target = "title", source = "product.name")
    @Mapping(target = "variantTitle", source = "variant.title")
    @Mapping(target = "vendor", source = "product.vendor")
    @Mapping(target = "unit", source = "variant.unit")
    @Mapping(target = "inventoryManagement", source = "variant.inventoryManagement")
    @Mapping(target = "inventoryPolicy", source = "variant.inventoryPolicy")
    @Mapping(target = "grams", source = "variant.grams")
    @Mapping(target = "requireShipping", source = "variant.requiresShipping")
    @Mapping(target = "taxable", source = "variant.taxable")
    @Mapping(target = "type", source = "type")
    public abstract CombinationLineItemComponent toLineItemComponent(ProductResponse.Product product,
                                                                     VariantResponse.Variant variant,
                                                                     BigDecimal quantity,
                                                                     BigDecimal baseQuantity,
                                                                     BigDecimal price,
                                                                     BigDecimal linePrice,
                                                                     BigDecimal remainder,
                                                                     VariantType type);

    public abstract CombinationLineItemComponent toLineItemComponent(LineItemComponent component);
}
