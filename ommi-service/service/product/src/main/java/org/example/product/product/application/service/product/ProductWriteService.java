package org.example.product.product.application.service.product;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.example.product.product.application.model.images.StoredImageResult;
import org.example.product.product.application.model.product.*;
import org.example.product.product.domain.inventory.model.InventoryItem;
import org.example.product.product.domain.inventory.model.InventoryLevel;
import org.example.product.product.domain.inventory.repository.InventoryItemRepository;
import org.example.product.product.domain.inventory.repository.InventoryLevelRepository;
import org.example.product.product.domain.product.model.*;
import org.example.product.product.domain.product.repository.ProductIdGenerator;
import org.example.product.product.domain.product.repository.ProductRepository;
import org.example.product.product.domain.product.repository.ProductRepositoryImpl;
import org.example.product.product.infrastructure.data.dao.ProductDao;
import org.example.product.product.infrastructure.data.dao.StoreDao;
import org.example.product.product.infrastructure.data.dto.Location;
import org.example.product.product.infrastructure.data.dto.StoreDto;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductWriteService {

    public static final String MANAGER = "bizweb";


    private final StoreDao storeDao;
    private final ProductDao productDao;

    private final ProductIdGenerator idGenerator;

    private final ProductRepository productRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryLevelRepository inventoryLevelRepository;

    private final ImageProcessorService imageProcessorService;

    @Transactional
    public ProductId create(int storeId, ProductRequest productRequest) throws ExecutionException, InterruptedException, IOException {

        if (log.isDebugEnabled()) {
            log.debug("Starting Create Product");
        }

        var store = findStoreById(storeId);
        var productCount = productDao.countProductInStore(storeId);
        if (productCount >= store.getMaxProduct()) {
            throw new IllegalArgumentException("product_maximum is " + store.getMaxProduct());
        }

        //
        ProductId productId = new ProductId(storeId, idGenerator.generateProductId());

        //images
        var storeImages = imageProcessorService.storeImage(storeId, productRequest.getImages());
        var images = buildNewImages(productRequest.getImages(), storeImages);

        //variants
        List<Variant> variants = new ArrayList<>();
        List<InventoryItem> inventoryItems = new ArrayList<>();
        List<InventoryLevel> inventoryLevels = new ArrayList<>();
        if (CollectionUtils.isEmpty(productRequest.getVariants())) {
            var variant = defaultVariant(
                    idGenerator.generateVariantId(),
                    idGenerator.generateInventoryItemId(),
                    productRequest.getDefaultVariantUnit()
            );
            variants.add(variant);
            inventoryItems.add(defaultInventoryItem(variant, productId));
            inventoryLevels.add(defaultInventoryLevel(variant, productId));
        } else {
            var variantCount = productRequest.getVariants().size();

            var generatedVariantIds = idGenerator.generateVariantIds(variantCount);
            var generatedInventoryItemIds = idGenerator.generateInventoryItemIds(variantCount);

            variants = buildNewVariantWhenCreateProduct(
                    productRequest.getVariants(), storeImages, images,
                    generatedVariantIds, generatedInventoryItemIds);
            inventoryItems = buildInventoryItems(variants, productRequest.getVariants(), productId);
            inventoryLevels = buildInventoryLevels(productRequest.getVariants(), storeId, inventoryItems);
        }

        // save inventoryItems
        inventoryItemRepository.saveAll(inventoryItems);

        if (CollectionUtils.isNotEmpty(inventoryLevels)) {
            inventoryLevelRepository.saveAll(inventoryLevels);
        }

        var product = new Product(
                productId,
                productRequest.getName(),
                buildProductGeneralInfo(productRequest, new ProductGeneralInfo()),
                productRequest.getContent(),
                productRequest.getTags(),
                variants,
                images,
                productRequest.getPublishedOn(),
                idGenerator,
                productRequest.getStatus()
        );

        productRepository.save(product);

        return product.getId();
    }

    private ProductGeneralInfo buildProductGeneralInfo(ProductRequest productRequest, ProductGeneralInfo productGeneralInfo) {
        return productGeneralInfo.toBuilder()
                .vendor(productRequest.getVendor())
                .productType(productRequest.getProductType())
                .summary(productRequest.getSummary())
                .metaTitle(productGeneralInfo.getMetaTitle())
                .metaDescription(productRequest.getMetaDescription())
                .build();
    }

    private List<InventoryLevel> buildInventoryLevels(List<ProductVariantRequest> variantRequests, int storeId, List<InventoryItem> inventoryItems) {
        if (CollectionUtils.isEmpty(variantRequests)) {
            return List.of();
        }

        List<Integer> locationIds = variantRequests.stream()
                .flatMap(request -> request.getInventoryQuantities().stream())
                .filter(inventoryQuantity -> inventoryQuantity.getLocationId() > 0)
                .map(inventoryQuantity -> (int) inventoryQuantity.getLocationId())
                .distinct()
                .toList();
        var locations = getLocations(storeId, locationIds);

        var inventoryItemIds = inventoryItems.stream().map(InventoryItem::getId).collect(Collectors.toCollection(LinkedList::new));

        var locationDefault = getDefaultLocation(storeId);
        var defaultLocationId = Optional.ofNullable(locationDefault).map(Location::getId).orElse(null);

        var inventoryLevels = new ArrayList<InventoryLevel>();
        for (var variantRequest : variantRequests) {
            var inventoryItemId = inventoryItemIds.removeFirst();
            var inventoryItem = inventoryItems.stream()
                    .filter(i -> i.getId() == inventoryItemId)
                    .findFirst()
                    .orElse(null);
            assert inventoryItem != null;
            var variantId = inventoryItem.getVariantId();
            var tracked = inventoryItem.isTracked();
            if (CollectionUtils.isEmpty(variantRequest.getInventoryQuantities())) {
                var inventoryQuantity = tracked ? variantRequest.getInventoryQuantity() : 0;
                var inventoryLevel = new InventoryLevel(
                        storeId, inventoryItem.getProductId(),
                        variantId, inventoryItemId, defaultLocationId,
                        BigDecimal.valueOf(inventoryQuantity), BigDecimal.valueOf(inventoryQuantity),
                        BigDecimal.ZERO, BigDecimal.ZERO);
                inventoryLevels.add(inventoryLevel);
            } else {
                buildInventoryLevelsForVariant(
                        variantRequest,
                        inventoryLevels, locations, defaultLocationId,
                        storeId, inventoryItem,
                        tracked
                );
            }
        }
        return inventoryLevels;
    }

    private void buildInventoryLevelsForVariant(
            ProductVariantRequest variantRequest,
            ArrayList<InventoryLevel> inventoryLevels,
            Map<Integer, Location> locations,
            Integer defaultLocationId,
            int storeId,
            InventoryItem inventoryItem,
            boolean tracked
    ) {
        validateOnHand(variantRequest.getInventoryQuantities());
        for (var inventoryQuantityReq : variantRequest.getInventoryQuantities()) {
            var locationId = inventoryQuantityReq.getLocationId();
            if (locationId < 0) {
                log.debug("Invalid locationId");
            }
            var validLocationId = (int) locationId;
            var location = locations.get(validLocationId);
            if (location == null) {
                log.debug("Not existed location with id = " + validLocationId);
                validLocationId = defaultLocationId;
            }

            BigDecimal quantity = tracked ? inventoryQuantityReq.getOnHand() : BigDecimal.ZERO;
            BigDecimal defaultQuantity = BigDecimal.ZERO;
            var inventoryLevel = new InventoryLevel(
                    storeId, inventoryItem.getProductId(),
                    inventoryItem.getVariantId(), inventoryItem.getId(),
                    validLocationId, quantity, quantity,
                    defaultQuantity, defaultQuantity
            );
            inventoryLevels.add(inventoryLevel);
        }
    }

    private void validateOnHand(List<InventoryQuantityRequest> inventoryQuantities) {
        boolean existedInvalid = inventoryQuantities.stream()
                .anyMatch(inventoryQuantity ->
                        inventoryQuantity.getOnHand() == null || inventoryQuantity.getOnHand().signum() < 0);
        if (existedInvalid) {
            throw new IllegalArgumentException();
        }
    }

    private Map<Integer, Location> getLocations(int storeId, List<Integer> locationIds) {
        if (CollectionUtils.isEmpty(locationIds)) {
            return Map.of();
        }

        return new HashMap<>();
    }

    private List<InventoryItem> buildInventoryItems(List<Variant> variants, List<ProductVariantRequest> variantRequests, ProductId productId) {
        if (CollectionUtils.isEmpty(variantRequests)) return List.of();

        var inventoryItems = new ArrayList<InventoryItem>();
        for (int i = 0; i < variantRequests.size(); i++) {
            var variantRequest = variantRequests.get(i);
            var variant = variants.get(i);

            var tracked = StringUtils.equals(variantRequest.getInventoryManagement(), MANAGER);
            var requireShipping = Optional.ofNullable(variantRequest.getRequireShipping()).orElse(true);

            verifyCostPriceInVariantRequest(productId.getStoreId(), variantRequest);

            var inventoryItem = new InventoryItem(
                    variant.getInventoryItemId(),
                    productId.getStoreId(),
                    productId.getId(),
                    variant.getId(),
                    variant.getIdentityInfo().getSku(),
                    variant.getIdentityInfo().getBarcode(),
                    tracked,
                    requireShipping,
                    BigDecimal.ZERO
            );
            inventoryItems.add(inventoryItem);
        }
        return inventoryItems;
    }

    private void verifyCostPriceInVariantRequest(int storeId, ProductVariantRequest variantRequest) {
        var costPrice = variantRequest.getPrice();

        // TODO: business verify
    }

    private List<Variant> buildNewVariantWhenCreateProduct(
            List<ProductVariantRequest> variantRequests,
            List<StoredImageResult> storeImages,
            List<Image> images,
            Deque<Integer> variantIds,
            Deque<Integer> inventoryItemIds
    ) {
        if (CollectionUtils.isEmpty(variantRequests)) return List.of();

        var variants = new ArrayList<Variant>();
        for (var variantRequest : variantRequests) {
            Image image = null;
            if (variantRequest.getImagePosition() != null
                    && variantRequest.getImagePosition() < images.size()) {
                var imagePosition = variantRequest.getImagePosition();
                var storedImage = storeImages.get(imagePosition);
                if (storedImage != null) {
                    image = images.stream()
                            .filter(i -> Objects.equals(i.getSrc(), storedImage.getSrc()))
                            .findFirst().orElse(null);
                }
            }

            variants.add(buildNewVariant(variantRequest, image, variantIds.removeFirst(), inventoryItemIds.removeFirst()));
        }

        return variants;
    }

    private Variant buildNewVariant(ProductVariantRequest variantRequest, Image image, Integer variantId, Integer inventoryItemId) {
        return new Variant(
                variantId,
                inventoryItemId,
                buildVariantOptionInfo(variantRequest, new VariantOptionInfo()),
                buildVariantIdentityInfo(variantRequest, new VariantIdentityInfo()),
                buildVariantPricingInfo(variantRequest, new VariantPricingInfo()),
                buildVariantInventoryManagementInfo(variantRequest, new VariantInventoryManagementInfo()),
                buildVariantPhysicalInfo(variantRequest, new VariantPhysicalInfo()),
                variantRequest.getInventoryQuantity(),
                Optional.ofNullable(image).map(Image::getId).orElse(null)
        );
    }

    private VariantPhysicalInfo buildVariantPhysicalInfo(ProductVariantRequest variantRequest, VariantPhysicalInfo variantPhysicalInfo) {
        return variantPhysicalInfo.toBuilder()
                .requireShipping(Optional.ofNullable(variantRequest.getRequireShipping()).orElse(false))
                .weight(Optional.ofNullable(variantRequest.getWeight()).orElse(0D))
                .weighUnit(variantRequest.getWeightUnit())
                .unit(variantRequest.getUnit())
                .build();
    }

    private VariantInventoryManagementInfo buildVariantInventoryManagementInfo(ProductVariantRequest variantRequest, VariantInventoryManagementInfo variantInventoryManagementInfo) {
        return variantInventoryManagementInfo.toBuilder()
                .inventoryManagement(variantRequest.getInventoryManagement())
                .inventoryPolicy(variantRequest.getInventoryPolicy())
                .build();
    }

    private VariantPricingInfo buildVariantPricingInfo(ProductVariantRequest variantRequest, VariantPricingInfo variantPricingInfo) {
        return variantPricingInfo.toBuilder()
                .price(variantRequest.getPrice())
                .compareAtPrice(variantRequest.getCompareAtPrice())
                .taxable(Optional.ofNullable(variantRequest.getTaxable()).orElse(false))
                .build();
    }

    private VariantIdentityInfo buildVariantIdentityInfo(ProductVariantRequest variantRequest, VariantIdentityInfo variantIdentityInfo) {
        return variantIdentityInfo.toBuilder()
                .barcode(variantRequest.getBarcode())
                .sku(variantRequest.getSku())
                .build();
    }

    private VariantOptionInfo buildVariantOptionInfo(ProductVariantRequest variantRequest, VariantOptionInfo variantOptionInfo) {
        if (variantOptionInfo == null) variantOptionInfo = new VariantOptionInfo();

        var optionInfoBuilder = variantOptionInfo.toBuilder()
                .option1(StringUtils.isBlank(variantRequest.getOption1())
                        ? VariantOptionInfo.DEFAULT_OPTION_VALUE
                        : variantOptionInfo.getOption1())
                .option2(variantRequest.getOption2())
                .option3(variantRequest.getOption3());

        return optionInfoBuilder.build();
    }

    private InventoryLevel defaultInventoryLevel(Variant variant, ProductId productId) {
        var defaultLocation = this.getDefaultLocation(productId.getStoreId());
        var locationId = defaultLocation == null ? null : defaultLocation.getId();
        return new InventoryLevel(
                productId.getStoreId(),
                productId.getId(),
                variant.getId(),
                variant.getInventoryItemId(),
                locationId,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );
    }

    //TODO: get location
    private Location getDefaultLocation(int storeId) {
        return Location.builder()
                .id(1)
                .build();
    }

    private InventoryItem defaultInventoryItem(Variant variant, ProductId productId) {
        var inventoryItem = new InventoryItem();
        inventoryItem.setId(variant.getInventoryItemId());
        inventoryItem.setVariantId(variant.getId());
        inventoryItem.setProductId(productId.getId());
        inventoryItem.setStoreId(productId.getStoreId());
        inventoryItem.setSku(variant.getIdentityInfo().getSku());
        inventoryItem.setBarcode(variant.getIdentityInfo().getBarcode());
        inventoryItem.setTracked(false);
        inventoryItem.setRequireShipping(true);
        inventoryItem.setCostPrice(BigDecimal.ZERO);
        return inventoryItem;
    }

    private Variant defaultVariant(int variantId, int inventoryItemId, String defaultVariantUnit) {
        return new Variant(
                variantId,
                inventoryItemId,
                new VariantOptionInfo(),
                new VariantIdentityInfo(),
                new VariantPricingInfo(),
                new VariantInventoryManagementInfo(),
                VariantPhysicalInfo.builder()
                        .unit(defaultVariantUnit)
                        .build(),
                0,
                null
        );
    }

    private List<Image> buildNewImages(List<ProductImageRequest> imageRequests, List<StoredImageResult> storeImages) {
        if (CollectionUtils.isEmpty(storeImages)
                || CollectionUtils.isEmpty(imageRequests)) {
            return List.of();
        }

        var images = new ArrayList<Image>();
        var validImageCount = storeImages.stream().filter(Objects::nonNull).count();
        var imageIds = idGenerator.generateImageIds(validImageCount);
        for (int i = 0; i < imageRequests.size(); i++) {
            var storeImage = storeImages.get(i);
            if (storeImage == null) continue;
            var imageReq = imageRequests.get(i);
            String alt = imageReq.getAlt();
            var position = imageReq.getPosition();

            var image = new Image(
                    imageIds.removeFirst(),
                    alt,
                    storeImage.getSrc(),
                    storeImage.getFileName(),
                    position,
                    ImagePhysicalInfo.builder()
                            .size(storeImage.getSize())
                            .width(storeImage.getWidth())
                            .height(storeImage.getHeight())
                            .build()
            );
            images.add(image);
        }

        return images;
    }

    private StoreDto findStoreById(int storeId) {
        var store = storeDao.getStoreById(storeId);
        if (store == null) {
            throw new IllegalArgumentException("Store not found");
        }
        return store;
    }

    // start region handle_event
    @Async
    @EventListener(classes = ProductRepositoryImpl.StoreProductSuccessAppEvent.class)
    public void handleProductStoreSuccess(ProductRepositoryImpl.StoreProductSuccessAppEvent event) {
        var product = event.product();
    }

    @Transactional
    public void updateProduct(ProductId productId, ProductRequest productRequest) throws IOException, ExecutionException, InterruptedException {
        var product = productRepository.findById(productId);
        if (product == null) throw new IllegalArgumentException("product not found");

        product.setName(productRequest.getName());

        product.setContent(productRequest.getContent());

        // update images
        processImageForUpdateProduct(productRequest, product);

        // update general infos
        product.setGeneralInfo(buildProductGeneralInfo(productRequest, product.getGeneralInfo()));

        var addAndUpdateVariantInfo = generateAddAndUpdateVariant(productRequest, product);

        var addVariants = addAndUpdateVariantInfo.newVariants;
        var updateVariants = addAndUpdateVariantInfo.updateVariants;
        product.setVariants(addVariants, updateVariants);

        var addAndUpdateVariantRequest = getAddAndUpdateVariantRequest(productRequest);

        List<InventoryItem> inventoryItemInserts = new ArrayList<>();
        List<InventoryItem> inventoryItemUpdates = new ArrayList<>();
        List<InventoryLevel> inventoryLevelInserts = new ArrayList<>();
        List<InventoryLevel> inventoryLevelUpdates = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(addAndUpdateVariantRequest.addVariants)
                && CollectionUtils.isNotEmpty(addVariants)) {
            inventoryItemInserts = buildInventoryItemAdds(addAndUpdateVariantRequest.addVariants, productId);
        }
        if (CollectionUtils.isNotEmpty(addAndUpdateVariantRequest.updateVariants)
                && !updateVariants.isEmpty()) {
            inventoryItemUpdates = buildInventoryItemUpdates(addAndUpdateVariantRequest.updateVariants, productId);
            var inventoryItemUpdateMap = inventoryItemUpdates.stream()
                    .collect(Collectors.toMap(
                            InventoryItem::getVariantId,
                            Function.identity(),
                            (first, second) -> second));

            var locationDefault = getDefaultLocation(productId.getStoreId());
            var defaultLocationId = Optional.ofNullable(locationDefault).map(Location::getId).orElse(null);

            var updateVariantIds = addAndUpdateVariantInfo.updateVariants.keySet();
            var inventoryItemUpdateIds = inventoryItemUpdates.stream()
                    .map(InventoryItem::getId)
                    .toList();

            // get inventoryLevels
            var inventoryLevelOlds = inventoryLevelRepository.getByStoreIdAndInventoryItemIdIn(productId.getStoreId(), inventoryItemUpdateIds);
            for (var variantId : updateVariantIds) {
                var updatableVariantInfo = addAndUpdateVariantInfo.updateVariants.get(variantId);
                List<InventoryLevel> inventoryLevelOldVariants = inventoryLevelOlds.stream()
                        .filter(i -> i.getVariantId() == variantId)
                        .toList();
                var inventoryLevelDefaultLocation = inventoryLevelOldVariants.stream()
                        .filter(i -> Objects.equals(i.getLocationId(), defaultLocationId))
                        .findFirst().orElse(null);
                // chỉ update cho 1 inventory quantity
                if (updatableVariantInfo.getInventoryQuantity() != null) {
                    if (inventoryLevelOldVariants.size() > 1) {
                        throw new IllegalArgumentException("Not support update for multi location");
                    }
                    int quantity = updatableVariantInfo.getInventoryQuantity();
                    var inventoryItem = inventoryItemUpdateMap.get(variantId);
                    if (inventoryLevelDefaultLocation != null && inventoryItem != null && inventoryItem.isTracked()) {
                        inventoryLevelDefaultLocation.setOnHand(BigDecimal.valueOf(quantity));
                        inventoryLevelDefaultLocation.setAvailable(BigDecimal.valueOf(quantity));
                        inventoryLevelUpdates.add(inventoryLevelDefaultLocation);
                    }
                }
            }
        }

        if (CollectionUtils.isNotEmpty(inventoryItemInserts)) {
            inventoryItemRepository.saveAll(inventoryItemInserts);
        }
        if (CollectionUtils.isNotEmpty(inventoryItemUpdates)) {
            inventoryItemRepository.saveAll(inventoryItemUpdates);
        }

        if (CollectionUtils.isNotEmpty(inventoryLevelInserts)) {
            inventoryLevelRepository.saveAll(inventoryLevelInserts);
        }
        if (CollectionUtils.isNotEmpty(inventoryLevelUpdates)) {
            inventoryLevelRepository.saveAll(inventoryLevelUpdates);
        }

        productRepository.save(product);
    }

    private List<InventoryItem> buildInventoryItemUpdates(List<ProductVariantRequest> updateVariantRequests, ProductId productId) {
        if (CollectionUtils.isEmpty(updateVariantRequests))
            return Collections.emptyList();

        var variantIds = updateVariantRequests.stream()
                .map(ProductVariantRequest::getId)
                .toList();
        var inventoryItemVariants = inventoryItemRepository.getByStoreIdAndVariantIdIn(productId.getStoreId(), variantIds);

        var inventoryItems = new ArrayList<InventoryItem>();
        for (ProductVariantRequest request : updateVariantRequests) {
            var variantId = request.getId();
            var inventoryItem = inventoryItemVariants.stream()
                    .filter(item -> item.getVariantId() == variantId)
                    .findFirst()
                    .orElse(null);
            if (inventoryItem == null) {
                log.warn("InventoryItem not existed with variantId = {}", variantId);
                continue;
            }

            if (!StringUtils.equals(inventoryItem.getSku(), request.getSku())) {
                inventoryItem.setSku(request.getSku());
            }

            if (!StringUtils.equals(inventoryItem.getBarcode(), request.getBarcode())) {
                inventoryItem.setBarcode(request.getBarcode());
            }

            if (!Objects.equals(inventoryItem.isRequireShipping(), Optional.ofNullable(request.getRequireShipping()).orElse(true))) {
                inventoryItem.setRequireShipping(Optional.ofNullable(request.getRequireShipping()).orElse(true));
            }

            inventoryItems.add(inventoryItem);
        }

        return inventoryItems;
    }

    private List<InventoryItem> buildInventoryItemAdds(List<ProductVariantRequest> variantRequests, ProductId productId) {
        if (CollectionUtils.isEmpty(variantRequests))
            return Collections.emptyList();

        var inventoryItems = new ArrayList<InventoryItem>();
        for (var variantReq : variantRequests) {

        }

        return inventoryItems;
    }

    private AddAndUpdateVariantModel generateAddAndUpdateVariant(ProductRequest productRequest, Product product) {

        var addAndUpdateVariantRequest = getAddAndUpdateVariantRequest(productRequest);

        //build new variants
        var newVariants = buildNewVariantWhenUpdateProduct(addAndUpdateVariantRequest.addVariants, product.getImages());

        var updatableVariants = new LinkedHashMap<Integer, VariantUpdatableInfo>();
        if (CollectionUtils.isNotEmpty(addAndUpdateVariantRequest.updateVariants)) {
            var productVariantMap = product.getVariants().stream()
                    .collect(Collectors.toMap(Variant::getId, Function.identity()));
            for (var variantRequest : addAndUpdateVariantRequest.updateVariants) {
                var variantIdRequest = variantRequest.getId();
                var variant = productVariantMap.get(variantIdRequest);
                if (variant == null) {
                    log.warn("Variant not existed with id = {}", variantIdRequest);
                    continue;
                }
                var updatableInfo = getVariantUpdatableInfo(variantRequest, variant);
                updatableVariants.put(variantIdRequest, updatableInfo);
            }
        }

        return new AddAndUpdateVariantModel(newVariants, updatableVariants);
    }

    private VariantUpdatableInfo getVariantUpdatableInfo(ProductVariantRequest variantRequest, Variant variant) {
        var updatableInfoBuilder = VariantUpdatableInfo.builder()
                .optionInfo(buildVariantOptionInfo(variantRequest, variant.getOptionInfo()))
                .identityInfo(buildVariantIdentityInfo(variantRequest, variant.getIdentityInfo()))
                .pricingInfo(buildVariantPricingInfo(variantRequest, variant.getPricingInfo()))
                .inventoryManagementInfo(buildVariantInventoryManagementInfo(variantRequest, variant.getInventoryManagementInfo()))
                .physicalInfo(buildVariantPhysicalInfo(variantRequest, variant.getPhysicalInfo()));

        updatableInfoBuilder
                .imageId(variantRequest.getImageId())
                .inventoryQuantity(variantRequest.getInventoryQuantity())
                .inventoryQuantityAdjustment(variantRequest.getInventoryQuantityAdjustment())
                .type(variantRequest.getType());

        return updatableInfoBuilder.build();
    }

    private List<Variant> buildNewVariantWhenUpdateProduct(List<ProductVariantRequest> addVariants, List<Image> images) {
        if (CollectionUtils.isEmpty(addVariants))
            return List.of();

        var variants = new ArrayList<Variant>();

        var variantCount = addVariants.size();
        var generatedVariantIds = idGenerator.generateVariantIds(variantCount);
        var generateInventoryItemIds = idGenerator.generateInventoryItemIds(variantCount);
        for (var variantReq : addVariants) {
            Image image = null;
            if (CollectionUtils.isNotEmpty(images)
                    && variantReq.getImageId() != null) {
                image = images.stream()
                        .filter(i -> Objects.equals(i.getId(), variantReq.getImageId()))
                        .findFirst()
                        .orElse(null);
            }

            variants.add(buildNewVariant(variantReq, image, generatedVariantIds.removeFirst(), generateInventoryItemIds.removeFirst()));
        }

        return variants;
    }

    private AddAndUpdateVariantRequest getAddAndUpdateVariantRequest(ProductRequest productRequest) {
        if (productRequest.getVariants() == null)
            return new AddAndUpdateVariantRequest(List.of(), List.of());

        List<ProductVariantRequest> addVariants = new ArrayList<>();
        List<ProductVariantRequest> updateVariants = new ArrayList<>();
        for (var variantRequest : productRequest.getVariants()) {
            var variantIdRequest = variantRequest.getId();
            if (variantIdRequest != null && variantIdRequest > 0) {
                updateVariants.add(variantRequest);
            } else {
                addVariants.add(variantRequest);
            }
        }
        return new AddAndUpdateVariantRequest(addVariants, updateVariants);
    }

    @Transactional
    public int createVariant(ProductId productId, ProductVariantRequest variantRequest) {
        var product = productRepository.findById(productId);
        if (product == null) throw new IllegalArgumentException();

        Image image = null;
        if (variantRequest.getImageId() != null) {
            final int imageId = variantRequest.getImageId();
            image = product.getImages().stream()
                    .filter(i -> i.getId() == imageId)
                    .findFirst()
                    .orElse(null);
        }
        var variant = buildNewVariant(variantRequest, image, idGenerator.generateVariantId(), idGenerator.generateInventoryItemId());
        var inventoryItems = buildInventoryItems(List.of(variant), List.of(variantRequest), productId);
        var inventoryLevels = buildInventoryLevels(List.of(variantRequest), productId.getStoreId(), inventoryItems);

        inventoryItemRepository.saveAll(inventoryItems);
        inventoryLevelRepository.saveAll(inventoryLevels);

        setInventoryQuantity(List.of(variant), inventoryLevels);

        product.addVariant(variant);

        productRepository.save(product);

        return variant.getId();
    }

    private void setInventoryQuantity(List<Variant> variants, List<InventoryLevel> inventoryLevels) {
        for (var variant : variants) {
            var quantity = inventoryLevels.stream()
                    .filter(i -> (i.getVariantId() == variant.getId())
                            && (i.getOnHand() != null))
                    .map(i -> i.getOnHand().intValue())
                    .reduce(0, Integer::sum);
            if (variant.getInventoryQuantity() != quantity) {
                variant.setInventoryQuantity(quantity);
            }
        }
    }

    private record AddAndUpdateVariantRequest(
            List<ProductVariantRequest> addVariants,
            List<ProductVariantRequest> updateVariants
    ) {
    }

    private record AddAndUpdateVariantModel(
            List<Variant> newVariants,
            LinkedHashMap<Integer, VariantUpdatableInfo> updateVariants
    ) {
    }

    private void processImageForUpdateProduct(ProductRequest productRequest, Product product) throws IOException, ExecutionException, InterruptedException {
        //NOTE: chỉ xoá image khi truền List.Empty()
        if (productRequest.getImages() == null)
            return;

        var storeId = product.getId().getStoreId();

        var currentImages = product.getImages();
        var currentImageIds = currentImages.stream().map(Image::getId).toList();

        var imageRequests = productRequest.getImages();

        var newImageRequests = new ArrayList<ProductImageRequest>();
        var updateImageRequests = new ArrayList<ProductImageRequest>();

        for (var imageRequest : imageRequests) {
            if (imageRequest.getId() != null && imageRequest.getId() > 0) {
                updateImageRequests.add(imageRequest);
            } else {
                newImageRequests.add(imageRequest);
            }
        }

        // store new images
        var storedNewImages = imageProcessorService.storeImage(storeId, newImageRequests);
        var newImages = buildNewImages(newImageRequests, storedNewImages);

        var updatableImages = buildUpdateImages(product, currentImages, updateImageRequests);

        product.setImages(newImages, updatableImages);
    }

    private LinkedHashMap<Integer, ImageUpdatableInfo> buildUpdateImages(Product product, List<Image> currentImages, List<ProductImageRequest> updateImageRequests) {
        var updatableImages = new LinkedHashMap<Integer, ImageUpdatableInfo>();
        var currentImageMap = currentImages == null ? Map.of() : currentImages.stream().collect(Collectors.toMap(Image::getId, Function.identity()));
        for (var imageReq : updateImageRequests) {
            var imageId = imageReq.getId();
            var currentImage = currentImageMap.get(imageId);
            if (currentImage == null) {
                log.warn("Current Image with id {} not found", imageId);
                continue;
            }

            var updatableInfo = ImageUpdatableInfo.builder()
                    .alt(imageReq.getAlt())
                    .position(imageReq.getPosition())
                    .build();
            updatableImages.put(imageId, updatableInfo);

            // update image for variant
            if (CollectionUtils.isNotEmpty(imageReq.getVariantIds())) {
                product.updateImageForVariant(imageId, imageReq.getVariantIds());
            }
        }

        return updatableImages;
    }
    // endregion handle_event
}
