package org.example.product.product.domain.product.repository;

import org.example.product.product.domain.product.model.Product;
import org.example.product.product.domain.product.model.ProductId;

public interface ProductRepository {

    void save(Product product);

    Product findById(ProductId productId);
}
