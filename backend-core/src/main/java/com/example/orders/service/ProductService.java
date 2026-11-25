package com.example.orders.service;

import com.example.orders.model.DomainException;
import com.example.orders.model.Money;
import com.example.orders.model.Product;
import com.example.orders.model.ProductId;
import com.example.orders.model.Sku;
import com.example.orders.repository.ProductRepository;

import java.util.Comparator;
import java.util.List;

public class ProductService {

    private final ProductRepository products;
    private final UnitOfWork tx;

    public ProductService(ProductRepository products, UnitOfWork tx) {
        this.products = products;
        this.tx = tx;
    }

    public List<Product> findAll() {
        return tx.inTransaction(() -> products.findAll().stream()
                .sorted(Comparator.comparing(p -> p.getSku().value()))
                .toList());
    }

    public Product get(ProductId id) {
        return tx.inTransaction(() -> load(id));
    }

    public Product create(Sku sku, String name, Money price) {
        return tx.inTransaction(() -> {
            if (products.findBySku(sku).isPresent()) {
                throw new DomainException("SKU " + sku + " existiert bereits");
            }
            return products.save(Product.create(sku, name, price));
        });
    }

    /** Die SKU ist ein fachlicher Schlüssel und bleibt unveränderlich. */
    public Product update(ProductId id, String name, Money price, boolean active) {
        return tx.inTransaction(() -> {
            Product product = load(id);
            product.rename(name);
            product.changePrice(price);
            product.setActive(active);
            return products.save(product);
        });
    }

    private Product load(ProductId id) {
        return products.findById(id).orElseThrow(() -> new NotFoundException("Produkt", id));
    }
}
