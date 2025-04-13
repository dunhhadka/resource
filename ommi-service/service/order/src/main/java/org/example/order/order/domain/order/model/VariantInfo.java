package org.example.order.order.domain.order.model;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Embeddable
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VariantInfo {
    private Integer variantId;
    private Integer productId;
    private boolean productExists;

    private @NotBlank @Size(max = 2000) String name;
    private @NotBlank @Size(max = 500) String title;
    private @Size(max = 1500) String variantTitle;
    private @Size(max = 255) String vendor;
    private @Size(max = 50) String sku;

    private @Min(0) int grams;
    private boolean requireShipping;

    private String inventoryManagement;
    private boolean restockable;

    private Integer inventoryItemId;
    private @Size(max = 50) String unit;
}
