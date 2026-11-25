package com.example.orders.controller;

import com.example.orders.model.DomainException;

import java.util.UUID;

final class Ids {

    private Ids() {
    }

    static UUID parse(String raw) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new DomainException("Ungültige ID: " + raw);
        }
    }
}
