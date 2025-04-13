package org.example.product.product.domain.combo.model;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Table(name = "Combos")
public class Combo {
    @Id
    @Column(updatable = false)
    private int variantId;
    private int storeId;
    private int productId;

    @OneToMany(mappedBy = "combo", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private List<@Valid ComboItem> comboItems = new ArrayList<>();
}
