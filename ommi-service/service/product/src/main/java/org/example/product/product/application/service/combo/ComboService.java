package org.example.product.product.application.service.combo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.example.product.product.application.model.combo.ComboItemRequest;
import org.example.product.product.application.model.combo.ComboRequest;
import org.example.product.product.domain.combo.model.Combo;
import org.example.product.product.domain.combo.model.ComboItem;
import org.example.product.product.domain.combo.repository.ComboRepository;
import org.example.product.product.domain.product.model.ProductId;
import org.example.product.product.domain.product.model.Variant;
import org.example.product.product.domain.product.repository.ProductRepository;
import org.example.product.product.infrastructure.data.dao.VariantDao;
import org.example.product.product.infrastructure.data.dto.VariantDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComboService {

    private final ComboRepository comboRepository;

    private final ProductRepository productRepository;
    private final VariantDao variantDao;

    @Transactional
    public void create(int storeId, ComboRequest comboRequest) {
        var variant = variantDao.getByVariantId(storeId, comboRequest.getVariantId());
        if (variant == null) {
            throw new IllegalArgumentException("");
        }

        if (variant.getType() != Variant.VariantType.normal) {
            throw new IllegalArgumentException();
        }

        var product = productRepository.findById(new ProductId(storeId, variant.getProductId()));
        if (product == null) {
            throw new IllegalArgumentException();
        }

        var existingCombo = comboRepository.getByStoreIdAndSubVariantId(storeId, variant.getId());
        if (CollectionUtils.isNotEmpty(existingCombo)) {
            throw new IllegalArgumentException();
        }

        var comboItemVariantIds = comboRequest.getComboItems().stream()
                .map(ComboItemRequest::getVariantId)
                .toList();
        if (comboItemVariantIds.contains(variant.getId())) {
            throw new IllegalArgumentException();
        }

        var comboItemVariants = variantDao.getByVariantIds(storeId, comboItemVariantIds);
        var comoItemMap = comboItemVariants.stream()
                .collect(Collectors.toMap(VariantDto::getId, Function.identity()));
        String notExisted = comboItemVariantIds.stream()
                .filter(id -> !comoItemMap.containsKey(id))
                .map(String::valueOf)
                .collect(Collectors.joining(", "));
        if (StringUtils.isNotEmpty(notExisted)) {
            throw new IllegalArgumentException(notExisted);
        }

        var comboItems = comboRequest.getComboItems().stream()
                .map(item -> {
                    var comboVariant = comoItemMap.get(item.getVariantId());
                    return new ComboItem();
                })
                .toList();
        var combo = new Combo();

        BigDecimal price = comboRequest.getPrice();
    }
}
