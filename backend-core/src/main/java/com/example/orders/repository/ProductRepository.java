package com.example.orders.repository;

import com.example.orders.model.Product;
import com.example.orders.model.ProductId;
import com.example.orders.model.Sku;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    Product save(Product product);
    Optional<Product> findById(ProductId id);
    Optional<Product> findBySku(Sku sku);
    List<Product> findAll();
    long count();
}
