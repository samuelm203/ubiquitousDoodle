package com.example.orders.app.ui;

import com.example.orders.model.DomainException;
import com.example.orders.service.NotFoundException;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;

/** Kleine Helfer, die alle Views gemeinsam nutzen. */
final class UiSupport {

    private static final Locale LOCALE = Locale.forLanguageTag("de-CH");
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", LOCALE).withZone(ZoneId.of("Europe/Zurich"));

    private UiSupport() {
    }

    /** Führt eine Controller-Aktion aus. Fachliche Fehler erscheinen als Notification statt als Fehlerseite. */
    static <T> Optional<T> call(Supplier<T> action) {
        try {
            return Optional.ofNullable(action.get());
        } catch (DomainException | NotFoundException e) {
            error(e.getMessage());
            return Optional.empty();
        }
    }

    static boolean run(Runnable action) {
        return call(() -> {
            action.run();
            return Boolean.TRUE;
        }).isPresent();
    }

    static void success(String message) {
        Notification.show(message, 3000, Notification.Position.BOTTOM_START)
                .addThemeVariants(NotificationVariant.SUCCESS);
    }

    static void error(String message) {
        Notification.show(message, 5000, Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.ERROR);
    }

    static String money(BigDecimal amount, String currency) {
        NumberFormat f = NumberFormat.getNumberInstance(LOCALE);
        f.setMinimumFractionDigits(2);
        f.setMaximumFractionDigits(2);
        return currency + " " + f.format(amount);
    }

    static String dateTime(Instant instant) {
        return instant == null ? "" : DATE_TIME.format(instant);
    }

    static String statusLabel(String status) {
        return switch (status) {
            case "DRAFT" -> "Entwurf";
            case "PLACED" -> "Aufgegeben";
            case "SHIPPED" -> "Versendet";
            case "CANCELLED" -> "Storniert";
            default -> status;
        };
    }
}
