package com.example.orders.jsf;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Currency;
import java.util.Locale;
import java.util.Map;

/** Formatierung für die Seiten: #{fmt.money(o.total, o.currency)}, #{fmt.dateTime(o.createdAt)}. */
@Named("fmt")
@ApplicationScoped
public class Fmt {

    private static final Locale LOCALE = Locale.of("de", "CH");
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withLocale(LOCALE).withZone(ZoneId.systemDefault());

    private static final Map<String, String> STATUS_LABELS = Map.of(
            "DRAFT", "Entwurf", "PLACED", "Aufgegeben", "SHIPPED", "Versendet", "CANCELLED", "Storniert");

    public String money(BigDecimal amount, String currency) {
        if (amount == null) {
            return "";
        }
        NumberFormat f = NumberFormat.getCurrencyInstance(LOCALE);
        f.setCurrency(Currency.getInstance(currency == null ? "CHF" : currency));
        return f.format(amount);
    }

    public String dateTime(Instant instant) {
        return instant == null ? "" : DATE_TIME.format(instant);
    }

    public String status(String status) {
        return STATUS_LABELS.getOrDefault(status, status);
    }

    /** CSS-Klasse des Status-Badges. */
    public String statusTone(String status) {
        return switch (status) {
            case "PLACED" -> "info";
            case "SHIPPED" -> "success";
            case "CANCELLED" -> "danger";
            default -> "neutral";
        };
    }
}
