package com.example.orders.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "products")
public class ProductEntity {

    @Id
    public UUID id;

    @Column(nullable = false, unique = true, length = 32)
    public String sku;

    @Column(nullable = false, length = 200)
    public String name;

    @Column(nullable = false, precision = 19, scale = 2)
    public BigDecimal price;

    @Column(nullable = false, length = 3)
    public String currency;

    @Column(nullable = false)
    public boolean active;
}
