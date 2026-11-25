package com.example.orders;

import com.example.orders.controller.CustomerController;
import com.example.orders.controller.DashboardController;
import com.example.orders.controller.OrderController;
import com.example.orders.controller.ProductController;
import com.example.orders.repository.CustomerRepository;
import com.example.orders.repository.OrderRepository;
import com.example.orders.repository.ProductRepository;
import com.example.orders.service.CustomerService;
import com.example.orders.service.DashboardService;
import com.example.orders.service.OrderService;
import com.example.orders.service.ProductService;
import com.example.orders.service.UnitOfWork;

import java.time.Clock;

/**
 * Verdrahtet das komplette Backend von Hand, ohne DI-Framework.
 * Jedes Framework (Spring, Quarkus, Micronaut, Javalin ...) braucht nur Repositories und
 * eine UnitOfWork bereitzustellen und kann dann die Controller aus diesem Objekt verwenden.
 */
public final class Backend {

    public final CustomerService customerService;
    public final ProductService productService;
    public final OrderService orderService;
    public final DashboardService dashboardService;

    public final CustomerController customers;
    public final ProductController products;
    public final OrderController orders;
    public final DashboardController dashboard;

    public Backend(CustomerRepository customerRepo, ProductRepository productRepo, OrderRepository orderRepo,
                   UnitOfWork tx, Clock clock) {
        customerService = new CustomerService(customerRepo, orderRepo, tx);
        productService = new ProductService(productRepo, tx);
        orderService = new OrderService(orderRepo, customerRepo, productRepo, tx, clock);
        dashboardService = new DashboardService(customerRepo, productRepo, orderRepo, tx);

        customers = new CustomerController(customerService);
        products = new ProductController(productService);
        orders = new OrderController(orderService, customerService);
        dashboard = new DashboardController(dashboardService);
    }
}
