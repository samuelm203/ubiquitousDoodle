package com.example.orders.model;

import java.util.Objects;

/** Model "Kunde". */
public class Customer {

    private final CustomerId id;
    private String name;
    private EmailAddress email;
    private String city;
    private boolean active;

    public Customer(CustomerId id, String name, EmailAddress email, String city, boolean active) {
        this.id = Objects.requireNonNull(id, "id");
        rename(name);
        changeEmail(email);
        relocate(city);
        this.active = active;
    }

    public static Customer register(String name, EmailAddress email, String city) {
        return new Customer(CustomerId.newId(), name, email, city, true);
    }

    public void rename(String newName) {
        if (newName == null || newName.isBlank()) {
            throw new DomainException("Der Kundenname darf nicht leer sein");
        }
        this.name = newName.strip();
    }

    public void changeEmail(EmailAddress newEmail) {
        this.email = Objects.requireNonNull(newEmail, "email");
    }

    public void relocate(String newCity) {
        this.city = newCity == null ? "" : newCity.strip();
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public CustomerId getId() { return id; }
    public String getName() { return name; }
    public EmailAddress getEmail() { return email; }
    public String getCity() { return city; }
    public boolean isActive() { return active; }

    @Override
    public boolean equals(Object o) {
        return o instanceof Customer other && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
