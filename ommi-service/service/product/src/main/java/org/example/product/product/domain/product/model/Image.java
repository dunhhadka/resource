package org.example.product.product.domain.product.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.product.ddd.NestedDomainEntity;
import org.example.product.product.application.model.product.ImageUpdatableInfo;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "Images")
public class Image extends NestedDomainEntity<Product> {

    @ManyToOne
    @JsonIgnore
    @Setter(AccessLevel.PACKAGE)
    @JoinColumn(name = "storeId", referencedColumnName = "storeId")
    @JoinColumn(name = "productId", referencedColumnName = "id")
    private Product aggRoot;

    @Id
    private int id;

    @Size(max = 255)
    private String alt;

    @NotNull
    private String src;

    @NotNull
    private String fileName;

    private Integer position;

    @Embedded
    @JsonUnwrapped
    private @Valid ImagePhysicalInfo physicalInfo;

    public Image(
            int id,
            String alt,
            String src,
            String fileName,
            Integer position,
            ImagePhysicalInfo physicalInfo
    ) {
        this.id = id;
        this.alt = alt;
        this.src = src;
        this.fileName = fileName;
        this.position = position;
        this.physicalInfo = physicalInfo;
    }

    public void update(ImageUpdatableInfo updatableInfo) {
        this.alt = updatableInfo.getAlt();
        this.src = updatableInfo.getSrc();
        this.fileName = updatableInfo.getFileName();
        this.position = updatableInfo.getPosition();

        this.physicalInfo = updatableInfo.getPhysicalInfo();
    }
}
