package com.example.orders.service;

import com.example.orders.model.Money;
import com.example.orders.model.Order;
import com.example.orders.model.OrderStatus;
import com.example.orders.repository.CustomerRepository;
import com.example.orders.repository.OrderRepository;
import com.example.orders.repository.ProductRepository;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class DashboardService {

    public record Stats(long customers, long products, Map<OrderStatus, Long> ordersByStatus, Money revenue) {}

    private final CustomerRepository customers;
    private final ProductRepository products;
    private final OrderRepository orders;
    private final UnitOfWork tx;

    public DashboardService(CustomerRepository customers, ProductRepository products,
                            OrderRepository orders, UnitOfWork tx) {
        this.customers = customers;
        this.products = products;
        this.orders = orders;
        this.tx = tx;
    }

    /** Umsatz = Summe aller aufgegebenen und versendeten Bestellungen. */
    public Stats stats() {
        return tx.inTransaction(() -> {
            List<Order> all = orders.findAll();
            Map<OrderStatus, Long> byStatus = new EnumMap<>(OrderStatus.class);
            for (OrderStatus s : OrderStatus.values()) {
                byStatus.put(s, all.stream().filter(o -> o.getStatus() == s).count());
            }
            Money revenue = all.stream()
                    .filter(o -> o.getStatus() == OrderStatus.PLACED || o.getStatus() == OrderStatus.SHIPPED)
                    .map(Order::total)
                    .reduce(Money.zero(), Money::plus);
            return new Stats(customers.count(), products.count(), byStatus, revenue);
        });
    }
}
