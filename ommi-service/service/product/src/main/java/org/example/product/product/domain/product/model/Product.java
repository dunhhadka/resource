package org.example.product.product.domain.product.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import jakarta.persistence.*;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.example.product.ddd.AggregateRoot;
import org.example.product.product.application.model.product.ImageUpdatableInfo;
import org.example.product.product.application.model.product.VariantUpdatableInfo;
import org.example.product.product.domain.product.repository.ProductIdGenerator;
import org.example.product.product.domain.product.rules.ProductLimitTag;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

@Slf4j
@Getter
@Entity
@Table(name = "Products")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends AggregateRoot<Product> {

    @JsonIgnore
    @Transient
    @Setter
    private ProductIdGenerator idGenerator;

    @EmbeddedId
    @JsonUnwrapped
    private ProductId id;

    @NotBlank
    @Size(max = 320)
    private String name;
    @NotBlank
    @Size(max = 150)
    private String alias;

    @Enumerated(value = EnumType.STRING)
    private ProductStatus status;

    private Instant publishedOn;

    @Enumerated(value = EnumType.STRING)
    private Type type;

    private Instant createdOn;
    private Instant modifiedOn;

    @Embedded
    @JsonUnwrapped
    private @Valid ProductGeneralInfo generalInfo = new ProductGeneralInfo();

    @Embedded
    @JsonUnwrapped
    private @Valid ProductPricingInfo pricingInfo = new ProductPricingInfo();

    private boolean available;

    @OneToMany(mappedBy = "aggRoot", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @Fetch(FetchMode.SUBSELECT)
    private List<Variant> variants = new ArrayList<>();

    @OneToMany(mappedBy = "aggRoot", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @Fetch(FetchMode.SUBSELECT)
    private List<Image> images = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "ProductTags", joinColumns = {
            @JoinColumn(name = "storeId", referencedColumnName = "storeId"),
            @JoinColumn(name = "productId", referencedColumnName = "id")
    })
    private List<Tag> tags = new ArrayList<>();

    public Product(
            ProductId id,
            String name,
            ProductGeneralInfo productGeneralInfo,
            String content,
            List<String> tags,
            List<Variant> variants,
            List<Image> images,
            Instant publishedOn,
            ProductIdGenerator idGenerator,
            ProductStatus status
    ) {
        this.idGenerator = idGenerator;
        this.id = id;
        this.name = name;

        this.generalInfo = productGeneralInfo;

        this.setContent(content);
        this.setTags(tags);

        this.setImages(images, null);

        this.setVariants(variants, null);

        this.initStatusAndPublishedOn(status, publishedOn);
    }

    private void initStatusAndPublishedOn(ProductStatus status, Instant publishedOn) {
        this.publishedOn = publishedOn;
        this.status = status == null ? ProductStatus.active : status;

        var now = Instant.now();
        if (this.status != ProductStatus.active) {
            this.publishedOn = null;
        } else if (publishedOn != null && publishedOn.isAfter(now)) {
            this.publishedOn = now;
        }
    }

    public void setVariants(List<Variant> newVariants, LinkedHashMap<Integer, VariantUpdatableInfo> updatableInfos) {
        if (CollectionUtils.isEmpty(newVariants)
                && (updatableInfos == null || updatableInfos.isEmpty())) return;

        if (newVariants == null) newVariants = new ArrayList<>();
        if (updatableInfos == null) updatableInfos = new LinkedHashMap<>();

        var updateVariantIds = updatableInfos.keySet();
        var needRemoveVariants = this.variants.stream()
                .filter(v -> !updateVariantIds.contains(v.getId()))
                .toList();
        if (CollectionUtils.isNotEmpty(needRemoveVariants)) {
            needRemoveVariants.forEach(this::internalRemoveVariant);
        }

        if (!updatableInfos.isEmpty()) {
            updatableInfos.forEach(this::internalUpdateVariant);
        }
    }

    private void internalUpdateVariant(Integer variantId, VariantUpdatableInfo variantUpdatableInfo) {
        var variant = this.variants.stream()
                .filter(v -> v.getId() == variantId)
                .findFirst()
                .orElse(null);
        if (variant == null) {
            log.warn("Variant not found with id = " + variantId);
            return;
        }

        variant.update(variantUpdatableInfo);
    }

    private void internalRemoveVariant(Variant variant) {
        if (!this.variants.contains(variant)) return;
        this.variants.remove(variant);
    }

    /**
     *
     */
    public void setImages(List<Image> newImages, LinkedHashMap<Integer, ImageUpdatableInfo> updatableInfos) {
        if (newImages == null && updatableInfos == null) return;
        if (newImages == null) newImages = new ArrayList<>();
        if (updatableInfos == null) updatableInfos = new LinkedHashMap<>();

        var updateImageIds = updatableInfos.keySet();
        var needRemoveImages = this.images.stream().filter(image -> !updateImageIds.contains(image.getId())).toList();
        if (CollectionUtils.isNotEmpty(needRemoveImages)) {
            needRemoveImages.forEach(this::internalRemoveImage);
        }

        for (var image : newImages) {
            internalAddImage(image);
        }

        if (!updatableInfos.isEmpty()) {
            updatableInfos.forEach(this::internalUpdateImage);
        }
    }

    private void internalUpdateImage(Integer imageId, ImageUpdatableInfo updatableInfo) {
        var image = this.images.stream()
                .filter(i -> Objects.equals(i.getId(), imageId))
                .findFirst()
                .orElse(null);
        if (image == null) {
            log.warn("Image not found with id = " + imageId);
            return;
        }

        image.update(updatableInfo);
    }

    private void internalAddImage(Image image) {
        image.setAggRoot(this);
        this.images.add(image);
    }

    private void internalRemoveImage(Image image) {
        if (!this.images.contains(image)) return;
        this.images.remove(image);

        this.variants.stream()
                .filter(variant -> Objects.equals(variant.getImageId(), image.getId()))
                .forEach(Variant::removeImage);
    }

    private void setTags(List<String> tagNames) {
        if (tagNames == null) return;

        this.checkRule("limit_tag", new ProductLimitTag(tagNames.size()));

        var tagsNeedRemoved = this.tags.stream()
                .filter(tag -> !tagNames.contains(tag.getName()))
                .toList();
        if (CollectionUtils.isNotEmpty(tagsNeedRemoved)) {
            for (var tag : tagsNeedRemoved) {
                internalRemoveTag(tag);
            }
        }

        for (var tagName : tagNames) {
            internalAddTag(tagName);
        }

        this.modifiedOn = Instant.now();
    }

    private void internalRemoveTag(Tag tag) {
        if (!this.tags.contains(tag)) return;
        this.tags.remove(tag);
    }

    private void internalAddTag(String tagName) {
        if (this.tags.stream().anyMatch(tag -> StringUtils.equals(tag.getName(), tagName))) return;
        var tag = new Tag(tagName, tagName);
        this.tags.add(tag);
    }

    public void setContent(String content) {
        if (StringUtils.isEmpty(content)) return;

    }

    public void setName(String name) {
        if (StringUtils.equals(this.name, name)) return;
        this.name = name;
        //TODO: add event
        this.modifiedOn = Instant.now();
    }

    public void updateImageForVariant(Integer imageId, List<Integer> variantIds) {
        var variantsForUpdateImage = this.variants.stream()
                .filter(variant -> variantIds.contains(variant.getId()))
                .toList();
        if (CollectionUtils.isNotEmpty(variantsForUpdateImage)) {
            log.warn("Skipping update image for variants with id = {}", variantIds);
            return;
        }
        if (imageId == null) {
            log.debug("remove image for variants");
            variantsForUpdateImage.forEach(Variant::removeImage);
            return;
        }

        var existedImage = this.images.stream().anyMatch(i -> i.getId() == imageId);
        if (!existedImage) {
            throw new IllegalArgumentException("image not found");
        }
        variantsForUpdateImage.forEach(variant -> variant.updateImage(imageId));
    }

    public void setGeneralInfo(ProductGeneralInfo productGeneralInfo) {
        if (productGeneralInfo == null || productGeneralInfo.sameAs(this.generalInfo)) return;

        this.generalInfo = productGeneralInfo;
        this.modifiedOn = Instant.now();
    }

    public void addVariant(Variant variant) {
        internalAddVariant(variant);

        applySideEffectAfterAddVariant();

        this.modifiedOn = Instant.now();
    }

    private void applySideEffectAfterAddVariant() {
        setAvailable();

        calculateAndSetPricingInfo();

        changeTypeByVariant();
    }

    private void changeTypeByVariant() {
        if (this.variants.stream().anyMatch(v -> v.getType() == Variant.VariantType.combo)) {
            this.type = Type.combo;
        } else if (this.variants.stream().anyMatch(v -> v.getType() == Variant.VariantType.packsize)) {
            this.type = Type.packsize;
        } else {
            this.type = Type.normal;
        }
    }

    private void calculateAndSetPricingInfo() {

    }

    private void setAvailable() {
        var checkAvailable = false;
        for (var variant : variants) {
            if (StringUtils.isBlank(variant.getInventoryManagementInfo().getInventoryManagement())) {
                checkAvailable = true;
                break;
            }
            if (variant.getInventoryQuantity() > 0) {
                checkAvailable = true;
                break;
            }
        }

        this.available = checkAvailable;
    }

    private void internalAddVariant(Variant variant) {
        variant.setAggRoot(this);
        this.variants.add(variant);
    }

    public enum ProductStatus {
        draft, active, archive
    }

    public enum Type {
        normal, combo, packsize
    }
}
