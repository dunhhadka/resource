package org.example.product.product.domain.product.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Tag {
    @NotBlank
    @Column(name = "tag")
    @Size(max = 255)
    private String name;

    @Column(name = "tagAlias")
    private String alias;
}
