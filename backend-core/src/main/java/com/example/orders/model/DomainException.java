package com.example.orders.model;

/** Verletzung einer fachlichen Regel. Jede View/API behandelt sie als "Benutzerfehler". */
public class DomainException extends RuntimeException {
    public DomainException(String message) {
        super(message);
    }
}
