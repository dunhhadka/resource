package org.example.product.product.application.model.product;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.example.product.product.domain.product.model.*;

@Getter
@Setter
@Builder
public class VariantUpdatableInfo {
    private VariantOptionInfo optionInfo;
    private VariantIdentityInfo identityInfo;
    private VariantPricingInfo pricingInfo;
    private VariantInventoryManagementInfo inventoryManagementInfo;
    private VariantPhysicalInfo physicalInfo;

    private Integer imageId;
    private Integer inventoryQuantity;
    private Integer oldInventoryQuantity;
    private Integer inventoryQuantityAdjustment;
    private Variant.VariantType type;
}
