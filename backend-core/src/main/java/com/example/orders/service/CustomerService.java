package com.example.orders.service;

import com.example.orders.model.Customer;
import com.example.orders.model.CustomerId;
import com.example.orders.model.DomainException;
import com.example.orders.model.EmailAddress;
import com.example.orders.repository.CustomerRepository;
import com.example.orders.repository.OrderRepository;

import java.util.Comparator;
import java.util.List;

/** Geschäftslogik rund um Kunden. Reines Java: Abhängigkeiten kommen über den Konstruktor. */
public class CustomerService {

    private final CustomerRepository customers;
    private final OrderRepository orders;
    private final UnitOfWork tx;

    public CustomerService(CustomerRepository customers, OrderRepository orders, UnitOfWork tx) {
        this.customers = customers;
        this.orders = orders;
        this.tx = tx;
    }

    public List<Customer> findAll() {
        return tx.inTransaction(() -> customers.findAll().stream()
                .sorted(Comparator.comparing(Customer::getName, String.CASE_INSENSITIVE_ORDER))
                .toList());
    }

    public Customer get(CustomerId id) {
        return tx.inTransaction(() -> load(id));
    }

    public Customer register(String name, EmailAddress email, String city) {
        return tx.inTransaction(() -> {
            requireUniqueEmail(email, null);
            return customers.save(Customer.register(name, email, city));
        });
    }

    public Customer update(CustomerId id, String name, EmailAddress email, String city, boolean active) {
        return tx.inTransaction(() -> {
            Customer customer = load(id);
            requireUniqueEmail(email, id);
            customer.rename(name);
            customer.changeEmail(email);
            customer.relocate(city);
            customer.setActive(active);
            return customers.save(customer);
        });
    }

    public void delete(CustomerId id) {
        tx.run(() -> {
            load(id);
            if (orders.existsByCustomer(id)) {
                throw new DomainException("Kunde hat Bestellungen und kann nur deaktiviert werden");
            }
            customers.delete(id);
        });
    }

    private Customer load(CustomerId id) {
        return customers.findById(id).orElseThrow(() -> new NotFoundException("Kunde", id));
    }

    private void requireUniqueEmail(EmailAddress email, CustomerId self) {
        customers.findByEmail(email)
                .filter(other -> !other.getId().equals(self))
                .ifPresent(other -> {
                    throw new DomainException("E-Mail " + email + " wird bereits verwendet");
                });
    }
}
