package org.example.order.order.domain.order.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.text.Normalizer;

@Getter
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class OrderTag {
    @NotBlank
    @Size(max = 250)
    @Column(name = "tag")
    private String name;
    @NotBlank
    @Size(max = 255)
    @Column(name = "tag_alias")
    private String alias;

    public OrderTag(String value) {
        this.name = value;
        this.alias = Normalizer.normalize(value, Normalizer.Form.NFKC);
    }
}
