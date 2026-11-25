package com.example.orders.model;

import java.util.Objects;

/**
 * Bestellposition. Name und Preis werden beim Hinzufügen "eingefroren", damit spätere
 * Preisänderungen bestehende Bestellungen nicht verändern.
 */
public record OrderLine(ProductId productId, String productName, Money unitPrice, int quantity) {

    public static final int MAX_QUANTITY = 1_000;

    public OrderLine {
        Objects.requireNonNull(productId, "productId");
        Objects.requireNonNull(productName, "productName");
        Objects.requireNonNull(unitPrice, "unitPrice");
        if (quantity <= 0 || quantity > MAX_QUANTITY) {
            throw new DomainException("Menge muss zwischen 1 und " + MAX_QUANTITY + " liegen");
        }
    }

    public Money total() {
        return unitPrice.times(quantity);
    }

    OrderLine withQuantity(int newQuantity) {
        return new OrderLine(productId, productName, unitPrice, newQuantity);
    }
}
