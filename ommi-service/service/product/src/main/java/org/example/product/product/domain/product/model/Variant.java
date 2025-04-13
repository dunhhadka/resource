package org.example.product.product.domain.product.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.product.ddd.NestedDomainEntity;
import org.example.product.product.application.model.product.VariantUpdatableInfo;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "Variants")
public class Variant extends NestedDomainEntity<Product> {

    @ManyToOne
    @JsonIgnore
    @Setter
    @JoinColumn(name = "storeId", referencedColumnName = "storeId")
    @JoinColumn(name = "productId", referencedColumnName = "id")
    private Product aggRoot;

    @Id
    private int id;

    private int inventoryItemId;

    @Enumerated(value = EnumType.STRING)
    private VariantType type;

    @JsonUnwrapped
    @Embedded
    @Valid
    private VariantIdentityInfo identityInfo;

    @JsonUnwrapped
    @Embedded
    @Valid
    private VariantPricingInfo pricingInfo;

    @JsonUnwrapped
    @Embedded
    @Valid
    private VariantOptionInfo optionInfo;

    @NotBlank
    @Size(max = 1500)
    private String title = VariantOptionInfo.DEFAULT_OPTION_VALUE;

    @JsonUnwrapped
    @Embedded
    @Valid
    private VariantInventoryManagementInfo inventoryManagementInfo;

    private int inventoryQuantity;

    @JsonUnwrapped
    @Embedded
    @Valid
    private VariantPhysicalInfo physicalInfo;

    private Integer imageId;

    public Variant(
            int id,
            int inventoryItemId,
            VariantOptionInfo optionInfo,
            VariantIdentityInfo identityInfo,
            VariantPricingInfo pricingInfo,
            VariantInventoryManagementInfo inventoryManagementInfo,
            VariantPhysicalInfo physicalInfo,
            int inventoryQuantity,
            Integer imageId
    ) {
        this.id = id;
        this.inventoryItemId = inventoryItemId;
        this.optionInfo = optionInfo;
        this.identityInfo = identityInfo;
        this.pricingInfo = pricingInfo;
        this.inventoryManagementInfo = inventoryManagementInfo;
        this.physicalInfo = physicalInfo;
        this.inventoryQuantity = inventoryQuantity;
        this.imageId = imageId;
    }

    public void removeImage() {
        this.imageId = null;
    }

    public void update(VariantUpdatableInfo updatableInfo) {
        if (updatableInfo.getIdentityInfo() != null
                && !this.getIdentityInfo().sameAs(updatableInfo.getIdentityInfo())) {
            this.identityInfo = updatableInfo.getIdentityInfo();
        }

        if (updatableInfo.getPricingInfo() != null
                && !this.getPricingInfo().sameAs(updatableInfo.getPricingInfo())) {
            this.pricingInfo = updatableInfo.getPricingInfo();
        }

        if (updatableInfo.getInventoryManagementInfo() != null
                && !this.getInventoryManagementInfo().sameAs(updatableInfo.getInventoryManagementInfo())) {
            this.inventoryManagementInfo = updatableInfo.getInventoryManagementInfo();
        }

        setOptionInfo(updatableInfo.getOptionInfo());

        this.imageId = updatableInfo.getImageId();
        this.type = updatableInfo.getType();
    }

    private void setOptionInfo(VariantOptionInfo optionInfo) {
        if (optionInfo == null || this.optionInfo.sameAs(optionInfo)) {
            return;
        }

        var diffs = this.optionInfo.getDiffs(optionInfo);

        this.optionInfo = optionInfo;
    }

    public void updateImage(Integer imageId) {
        this.imageId = imageId;
    }

    public void setInventoryQuantity(Integer quantity) {
        this.inventoryQuantity = quantity;
    }

    public enum VariantType {
        normal, combo, packsize
    }
}
