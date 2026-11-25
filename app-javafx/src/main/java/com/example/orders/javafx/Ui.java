package com.example.orders.javafx;

import com.example.orders.model.DomainException;
import com.example.orders.service.NotFoundException;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/** Kleine Helfer, die alle Tabs gemeinsam nutzen. */
final class Ui {

    private static final Locale LOCALE = Locale.forLanguageTag("de-CH");
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", LOCALE).withZone(ZoneId.of("Europe/Zurich"));

    private Ui() {
    }

    static <T> TableColumn<T, String> column(String title, double width, Function<T, String> value) {
        TableColumn<T, String> col = new TableColumn<>(title);
        col.setPrefWidth(width);
        col.setCellValueFactory(cell -> new ReadOnlyStringWrapper(value.apply(cell.getValue())));
        return col;
    }

    /** Führt eine Controller-Aktion aus. Fachliche Fehler erscheinen als Dialog statt als Stacktrace. */
    static <T> Optional<T> call(Supplier<T> action) {
        try {
            return Optional.ofNullable(action.get());
        } catch (DomainException | NotFoundException e) {
            Alert alert = new Alert(Alert.AlertType.WARNING, e.getMessage());
            alert.setHeaderText(null);
            alert.showAndWait();
            return Optional.empty();
        }
    }

    static boolean run(Runnable action) {
        return call(() -> {
            action.run();
            return Boolean.TRUE;
        }).isPresent();
    }

    static boolean confirm(String question) {
        return new Alert(Alert.AlertType.CONFIRMATION, question).showAndWait()
                .filter(b -> b == javafx.scene.control.ButtonType.OK).isPresent();
    }

    static TextField field(String prompt) {
        TextField f = new TextField();
        f.setPromptText(prompt);
        return f;
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
