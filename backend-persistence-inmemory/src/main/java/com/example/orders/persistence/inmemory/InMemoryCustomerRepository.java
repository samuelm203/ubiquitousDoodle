package com.example.orders.persistence.inmemory;

import com.example.orders.model.Customer;
import com.example.orders.model.CustomerId;
import com.example.orders.model.EmailAddress;
import com.example.orders.repository.CustomerRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryCustomerRepository implements CustomerRepository {

    private final Map<CustomerId, Customer> store = new ConcurrentHashMap<>();

    @Override
    public Customer save(Customer customer) {
        store.put(customer.getId(), customer);
        return customer;
    }

    @Override
    public Optional<Customer> findById(CustomerId id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<Customer> findByEmail(EmailAddress email) {
        return store.values().stream().filter(c -> c.getEmail().equals(email)).findFirst();
    }

    @Override
    public List<Customer> findAll() {
        return List.copyOf(store.values());
    }

    @Override
    public void delete(CustomerId id) {
        store.remove(id);
    }

    @Override
    public long count() {
        return store.size();
    }
}
