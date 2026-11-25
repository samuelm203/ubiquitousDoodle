package com.example.orders.model;

import java.util.Objects;

/** Model "Produkt". */
public class Product {

    private final ProductId id;
    private final Sku sku;
    private String name;
    private Money price;
    private boolean active;

    public Product(ProductId id, Sku sku, String name, Money price, boolean active) {
        this.id = Objects.requireNonNull(id, "id");
        this.sku = Objects.requireNonNull(sku, "sku");
        rename(name);
        changePrice(price);
        this.active = active;
    }

    public static Product create(Sku sku, String name, Money price) {
        return new Product(ProductId.newId(), sku, name, price, true);
    }

    public void rename(String newName) {
        if (newName == null || newName.isBlank()) {
            throw new DomainException("Der Produktname darf nicht leer sein");
        }
        this.name = newName.strip();
    }

    public void changePrice(Money newPrice) {
        Objects.requireNonNull(newPrice, "price");
        if (newPrice.isNegative()) {
            throw new DomainException("Der Preis darf nicht negativ sein");
        }
        this.price = newPrice;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public ProductId getId() { return id; }
    public Sku getSku() { return sku; }
    public String getName() { return name; }
    public Money getPrice() { return price; }
    public boolean isActive() { return active; }

    @Override
    public boolean equals(Object o) {
        return o instanceof Product other && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
