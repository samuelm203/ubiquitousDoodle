package com.example.orders.jsf;

import com.example.orders.Backend;
import com.example.orders.controller.dto.Dtos.CustomerDto;
import com.example.orders.controller.dto.Dtos.CustomerForm;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Named
@ViewScoped
public class CustomersBean implements Serializable {

    @Inject
    transient Backend backend;

    private String filter = "";

    // Formularzustand des Dialogs; id == null bedeutet "neuer Kunde"
    private String id;
    private String name;
    private String email;
    private String city;
    private boolean active = true;

    public List<CustomerDto> getVisible() {
        String q = filter == null ? "" : filter.trim().toLowerCase(Locale.ROOT);
        return backend.customers.list().stream()
                .sorted(Comparator.comparing(CustomerDto::name, String.CASE_INSENSITIVE_ORDER))
                .filter(c -> q.isEmpty() || contains(c.name(), q) || contains(c.email(), q) || contains(c.city(), q))
                .toList();
    }

    private static boolean contains(String value, String q) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(q);
    }

    public void newCustomer() {
        id = null;
        name = "";
        email = "";
        city = "";
        active = true;
    }

    public void edit(CustomerDto c) {
        id = c.id();
        name = c.name();
        email = c.email();
        city = c.city() == null ? "" : c.city();
        active = c.active();
    }

    /** Gibt true zurück, wenn der Dialog geschlossen werden kann (wird per oncomplete ausgewertet). */
    public void save() {
        CustomerForm form = new CustomerForm(name, email, city == null || city.isBlank() ? null : city, active);
        boolean creating = id == null;
        Object result = Messages.attempt(
                () -> creating ? backend.customers.create(form) : backend.customers.update(id, form),
                creating ? "Kunde angelegt" : "Kunde gespeichert");
        if (result != null) {
            org.primefaces.PrimeFaces.current().executeScript("PF('customerDialog').hide()");
        }
    }

    public void delete() {
        String target = id;
        Object ok = Messages.attempt(() -> {
            backend.customers.delete(target);
            return Boolean.TRUE;
        }, "Kunde gelöscht");
        if (Objects.nonNull(ok)) {
            org.primefaces.PrimeFaces.current().executeScript("PF('customerDialog').hide()");
        }
    }

    public String getFilter() { return filter; }
    public void setFilter(String filter) { this.filter = filter; }
    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
