package com.example.orders.model;

import java.util.Locale;
import java.util.regex.Pattern;

public record EmailAddress(String value) {

    private static final Pattern SIMPLE_EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public EmailAddress {
        if (value == null || !SIMPLE_EMAIL.matcher(value.strip()).matches()) {
            throw new DomainException("Ungültige E-Mail-Adresse: " + value);
        }
        value = value.strip().toLowerCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return value;
    }
}
