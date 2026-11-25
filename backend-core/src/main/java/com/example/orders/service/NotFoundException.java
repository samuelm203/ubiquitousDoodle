package com.example.orders.service;

public class NotFoundException extends RuntimeException {
    public NotFoundException(String what, Object id) {
        super(what + " nicht gefunden: " + id);
    }
}
