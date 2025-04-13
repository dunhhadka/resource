package org.example.product.product.domain.combo.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Entity
@Table(name = "ComboItems")
public class ComboItem {

    @JsonIgnore
    @Setter
    @ManyToOne
    @JoinColumn(name = "comboVariantId", referencedColumnName = "variantId")
    private Combo combo;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private int variantId;
    private int productId;
    private int storeId;
    private int quantity;
}
