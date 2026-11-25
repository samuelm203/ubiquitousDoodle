package com.example.orders.persistence.inmemory;

import com.example.orders.model.Product;
import com.example.orders.model.ProductId;
import com.example.orders.model.Sku;
import com.example.orders.repository.ProductRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryProductRepository implements ProductRepository {

    private final Map<ProductId, Product> store = new ConcurrentHashMap<>();

    @Override
    public Product save(Product product) {
        store.put(product.getId(), product);
        return product;
    }

    @Override
    public Optional<Product> findById(ProductId id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<Product> findBySku(Sku sku) {
        return store.values().stream().filter(p -> p.getSku().equals(sku)).findFirst();
    }

    @Override
    public List<Product> findAll() {
        return List.copyOf(store.values());
    }

    @Override
    public long count() {
        return store.size();
    }
}
