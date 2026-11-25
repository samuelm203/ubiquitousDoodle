package com.example.orders.persistence.jpa;

import com.example.orders.model.Money;
import com.example.orders.model.Product;
import com.example.orders.model.ProductId;
import com.example.orders.model.Sku;
import com.example.orders.persistence.jpa.entity.ProductEntity;
import com.example.orders.repository.ProductRepository;
import jakarta.persistence.EntityManager;

import java.util.Currency;
import java.util.List;
import java.util.Optional;

public class JpaProductRepository implements ProductRepository {

    private final EntityManager em;

    public JpaProductRepository(EntityManager em) {
        this.em = em;
    }

    @Override
    public Product save(Product product) {
        ProductEntity existing = em.find(ProductEntity.class, product.getId().value());
        ProductEntity e = existing != null ? existing : new ProductEntity();
        e.id = product.getId().value();
        e.sku = product.getSku().value();
        e.name = product.getName();
        e.price = product.getPrice().amount();
        e.currency = product.getPrice().currency().getCurrencyCode();
        e.active = product.isActive();
        if (existing == null) {
            em.persist(e);
        }
        return product;
    }

    @Override
    public Optional<Product> findById(ProductId id) {
        return Optional.ofNullable(em.find(ProductEntity.class, id.value())).map(JpaProductRepository::toModel);
    }

    @Override
    public Optional<Product> findBySku(Sku sku) {
        return em.createQuery("select p from ProductEntity p where p.sku = :sku", ProductEntity.class)
                .setParameter("sku", sku.value())
                .getResultStream().findFirst().map(JpaProductRepository::toModel);
    }

    @Override
    public List<Product> findAll() {
        return em.createQuery("select p from ProductEntity p", ProductEntity.class)
                .getResultStream().map(JpaProductRepository::toModel).toList();
    }

    @Override
    public long count() {
        return em.createQuery("select count(p) from ProductEntity p", Long.class).getSingleResult();
    }

    private static Product toModel(ProductEntity e) {
        return new Product(new ProductId(e.id), new Sku(e.sku), e.name,
                new Money(e.price, Currency.getInstance(e.currency)), e.active);
    }
}
