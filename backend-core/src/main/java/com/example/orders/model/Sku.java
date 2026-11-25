package com.example.orders.model;

import java.util.Locale;
import java.util.regex.Pattern;

/** Stock Keeping Unit, z. B. "LAP-001". */
public record Sku(String value) {

    private static final Pattern FORMAT = Pattern.compile("^[A-Z0-9][A-Z0-9-]{2,31}$");

    public Sku {
        String normalized = value == null ? "" : value.strip().toUpperCase(Locale.ROOT);
        if (!FORMAT.matcher(normalized).matches()) {
            throw new DomainException("Ungültige SKU '" + value + "' (3-32 Zeichen, A-Z, 0-9, '-')");
        }
        value = normalized;
    }

    @Override
    public String toString() {
        return value;
    }
}
