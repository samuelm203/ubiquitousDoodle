package com.example.orders.persistence.inmemory;

import com.example.orders.model.CustomerId;
import com.example.orders.model.Order;
import com.example.orders.model.OrderId;
import com.example.orders.repository.OrderRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryOrderRepository implements OrderRepository {

    private final Map<OrderId, Order> store = new ConcurrentHashMap<>();

    @Override
    public Order save(Order order) {
        store.put(order.getId(), order);
        return order;
    }

    @Override
    public Optional<Order> findById(OrderId id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Order> findAll() {
        return List.copyOf(store.values());
    }

    @Override
    public boolean existsByCustomer(CustomerId customerId) {
        return store.values().stream().anyMatch(o -> o.getCustomerId().equals(customerId));
    }
}
