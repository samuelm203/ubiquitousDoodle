package com.example.orders.service;

import com.example.orders.model.Customer;
import com.example.orders.model.CustomerId;
import com.example.orders.model.DomainException;
import com.example.orders.model.Order;
import com.example.orders.model.OrderId;
import com.example.orders.model.Product;
import com.example.orders.model.ProductId;
import com.example.orders.repository.CustomerRepository;
import com.example.orders.repository.OrderRepository;
import com.example.orders.repository.ProductRepository;

import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

public class OrderService {

    private final OrderRepository orders;
    private final CustomerRepository customers;
    private final ProductRepository products;
    private final UnitOfWork tx;
    private final Clock clock;

    public OrderService(OrderRepository orders, CustomerRepository customers, ProductRepository products,
                        UnitOfWork tx, Clock clock) {
        this.orders = orders;
        this.customers = customers;
        this.products = products;
        this.tx = tx;
        this.clock = clock;
    }

    /** Neueste zuerst. */
    public List<Order> findAll() {
        return tx.inTransaction(() -> orders.findAll().stream()
                .sorted(Comparator.comparing(Order::getCreatedAt).reversed())
                .toList());
    }

    public Order get(OrderId id) {
        return tx.inTransaction(() -> load(id));
    }

    public Order createDraft(CustomerId customerId) {
        return tx.inTransaction(() -> {
            Customer customer = customers.findById(customerId)
                    .orElseThrow(() -> new NotFoundException("Kunde", customerId));
            if (!customer.isActive()) {
                throw new DomainException("Für inaktive Kunden können keine Bestellungen erfasst werden");
            }
            return orders.save(Order.draft(customerId, clock.instant()));
        });
    }

    public Order addProduct(OrderId orderId, ProductId productId, int quantity) {
        return change(orderId, order -> {
            Product product = products.findById(productId)
                    .orElseThrow(() -> new NotFoundException("Produkt", productId));
            order.addProduct(product, quantity);
        });
    }

    public Order removeProduct(OrderId orderId, ProductId productId) {
        return change(orderId, order -> order.removeProduct(productId));
    }

    public Order place(OrderId orderId) {
        return change(orderId, order -> order.place(clock.instant()));
    }

    public Order ship(OrderId orderId) {
        return change(orderId, Order::ship);
    }

    public Order cancel(OrderId orderId) {
        return change(orderId, Order::cancel);
    }

    private Order change(OrderId id, Consumer<Order> action) {
        return tx.inTransaction(() -> {
            Order order = load(id);
            action.accept(order);
            return orders.save(order);
        });
    }

    private Order load(OrderId id) {
        return orders.findById(id).orElseThrow(() -> new NotFoundException("Bestellung", id));
    }
}
