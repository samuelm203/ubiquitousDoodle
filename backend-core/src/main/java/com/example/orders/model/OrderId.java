package com.example.orders.model;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public record OrderId(UUID value) {
    public OrderId {
        Objects.requireNonNull(value, "value");
    }

    public static OrderId newId() {
        return new OrderId(UUID.randomUUID());
    }

    /** Kurze, menschenlesbare Darstellung für UIs. */
    public String shortForm() {
        return value.toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
