package org.example.product.product.domain.product.model;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.product.ddd.ValueObject;
import org.example.product.product.application.common.StringInList;

@Getter
@Builder(toBuilder = true)
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class VariantInventoryManagementInfo extends ValueObject<VariantInventoryManagementInfo> {
    @Builder.Default
    @StringInList(array = {"", "bizweb"}, allowBlank = true)
    @Size(max = 20)
    private String inventoryManagement = "";

    @Builder.Default
    @Size(max = 20)
    @StringInList(array = {"deny", "continue"})
    private String inventoryPolicy = "deny";
}
