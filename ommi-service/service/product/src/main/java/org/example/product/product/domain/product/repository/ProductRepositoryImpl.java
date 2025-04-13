package org.example.product.product.domain.product.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.product.ddd.DomainEvent;
import org.example.product.product.domain.product.model.Product;
import org.example.product.product.domain.product.model.ProductId;
import org.example.product.product.domain.product.model.ProductLog;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepository {

    @PersistenceContext
    private final EntityManager entityManager;

    private final ObjectMapper objectMapper;

    private final ProductIdGenerator productIdGenerator;

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void save(Product product) {
        var isNew = product.isNew();

        try {
            var productLog = generateLog(product, isNew ? ProductLog.VERB_ADD : ProductLog.VERB_UPDATE);

            if (isNew) {
                entityManager.persist(product);
            } else {
                entityManager.merge(product);
            }

            entityManager.persist(productLog);

            entityManager.flush();

            var event = new StoreProductSuccessAppEvent(isNew, product, product.getDomainEvents());
            applicationEventPublisher.publishEvent(event);
        } catch (Exception ex) {
            log.error("Saving product has error {}", product);
        }
    }

    @Override
    public Product findById(ProductId productId) {
        var product = entityManager.find(Product.class, productId);
        if (product != null) {
            product.setIdGenerator(productIdGenerator);
        }
        return product;
    }

    private ProductLog generateLog(Product product, String verb) throws JsonProcessingException {
        var productLog = new ProductLog();
        productLog.setProductId(product.getId().getId());
        productLog.setStoreId(product.getId().getStoreId());
        productLog.setVerb(verb);
        productLog.setCreatedOn(Instant.now());
        productLog.setData(objectMapper.writeValueAsString(product));
        return productLog;
    }

    public record StoreProductSuccessAppEvent(
            boolean isNew,
            Product product,
            List<DomainEvent> domainEvents
    ) {
    }
}
