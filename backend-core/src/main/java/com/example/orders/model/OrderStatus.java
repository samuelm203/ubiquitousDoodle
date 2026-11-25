package com.example.orders.model;

import java.util.Set;

/** Lebenszyklus einer Bestellung inkl. erlaubter Übergänge. */
public enum OrderStatus {
    DRAFT, PLACED, SHIPPED, CANCELLED;

    public boolean canTransitionTo(OrderStatus target) {
        return switch (this) {
            case DRAFT -> Set.of(PLACED, CANCELLED).contains(target);
            case PLACED -> Set.of(SHIPPED, CANCELLED).contains(target);
            case SHIPPED, CANCELLED -> false;
        };
    }
}
