package com.example.orders.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;
import java.util.UUID;

@Embeddable
public class OrderLineEmbeddable {

    @Column(name = "product_id", nullable = false)
    public UUID productId;

    @Column(name = "product_name", nullable = false, length = 200)
    public String productName;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 2)
    public BigDecimal unitPrice;

    @Column(nullable = false, length = 3)
    public String currency;

    @Column(nullable = false)
    public int quantity;
}
