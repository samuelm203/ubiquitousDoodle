package com.example.orders.jsf;

import com.example.orders.model.DomainException;
import com.example.orders.service.NotFoundException;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;

import java.util.function.Supplier;

/** Übersetzt fachliche Fehler des Backends in FacesMessages (Anzeige über p:growl). */
final class Messages {

    private Messages() {
    }

    static void info(String text) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, text, null));
    }

    static void error(String text) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, text, null));
    }

    /** Führt die Aktion aus. Bei fachlichem Fehler: Meldung anzeigen, null zurückgeben. */
    static <T> T attempt(Supplier<T> action, String successMessage) {
        try {
            T result = action.get();
            if (successMessage != null) {
                info(successMessage);
            }
            return result;
        } catch (DomainException | NotFoundException | IllegalArgumentException e) {
            error(e.getMessage());
            return null;
        }
    }
}
