package com.example.orders.repository;

import com.example.orders.model.CustomerId;
import com.example.orders.model.Order;
import com.example.orders.model.OrderId;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    Order save(Order order);
    Optional<Order> findById(OrderId id);
    List<Order> findAll();
    boolean existsByCustomer(CustomerId customerId);
}
