package com.example.orders.swing;

import com.example.orders.model.DomainException;
import com.example.orders.service.NotFoundException;

import javax.swing.JOptionPane;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;

/** Kleine Helfer, die alle Panels gemeinsam nutzen. */
public final class UiSupport {

    /** Dialoge sind austauschbar, damit Tests ohne sichtbares Fenster laufen. */
    public interface Dialogs {
        void error(String message);

        boolean confirm(String message);
    }

    private static final Locale LOCALE = Locale.forLanguageTag("de-CH");
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", LOCALE).withZone(ZoneId.of("Europe/Zurich"));

    private static volatile Dialogs dialogs = new Dialogs() {
        @Override
        public void error(String message) {
            JOptionPane.showMessageDialog(null, message, "Fehler", JOptionPane.ERROR_MESSAGE);
        }

        @Override
        public boolean confirm(String message) {
            return JOptionPane.showConfirmDialog(null, message, "Bestätigen",
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
        }
    };

    private UiSupport() {
    }

    public static void setDialogs(Dialogs replacement) {
        dialogs = replacement;
    }

    /** Führt eine Controller-Aktion aus. Fachliche Fehler erscheinen als Dialog. */
    public static <T> Optional<T> call(Supplier<T> action) {
        try {
            return Optional.ofNullable(action.get());
        } catch (DomainException | NotFoundException e) {
            error(e.getMessage());
            return Optional.empty();
        }
    }

    public static boolean run(Runnable action) {
        return call(() -> {
            action.run();
            return Boolean.TRUE;
        }).isPresent();
    }

    public static void error(String message) {
        dialogs.error(message);
    }

    public static boolean confirm(String message) {
        return dialogs.confirm(message);
    }

    /** Betrag samt Währung; sortierbar über den Betrag, angezeigt über toString. */
    public record Money(BigDecimal amount, String currency) implements Comparable<Money> {
        @Override
        public int compareTo(Money o) {
            return amount.compareTo(o.amount);
        }

        @Override
        public String toString() {
            return money(amount, currency);
        }
    }

    public static String money(BigDecimal amount, String currency) {
        NumberFormat f = NumberFormat.getNumberInstance(LOCALE);
        f.setMinimumFractionDigits(2);
        f.setMaximumFractionDigits(2);
        return currency + " " + f.format(amount);
    }

    public static String dateTime(Instant instant) {
        return instant == null ? "" : DATE_TIME.format(instant);
    }

    public static String statusLabel(String status) {
        return switch (status) {
            case "DRAFT" -> "Entwurf";
            case "PLACED" -> "Aufgegeben";
            case "SHIPPED" -> "Versendet";
            case "CANCELLED" -> "Storniert";
            default -> status;
        };
    }
}
