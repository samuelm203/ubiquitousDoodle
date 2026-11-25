package com.example.orders.repository;

import com.example.orders.model.Customer;
import com.example.orders.model.CustomerId;
import com.example.orders.model.EmailAddress;

import java.util.List;
import java.util.Optional;

/** Persistenz-Schnittstelle. Implementierungen liegen in den backend-persistence-* Modulen. */
public interface CustomerRepository {
    Customer save(Customer customer);
    Optional<Customer> findById(CustomerId id);
    Optional<Customer> findByEmail(EmailAddress email);
    List<Customer> findAll();
    void delete(CustomerId id);
    long count();
}
