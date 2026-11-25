package com.example.orders.controller;

import com.example.orders.controller.dto.Dtos.AddLineRequest;
import com.example.orders.controller.dto.Dtos.CreateOrderRequest;
import com.example.orders.controller.dto.Dtos.OrderDto;
import com.example.orders.controller.dto.Dtos.OrderLineDto;
import com.example.orders.model.Customer;
import com.example.orders.model.CustomerId;
import com.example.orders.model.Order;
import com.example.orders.model.OrderId;
import com.example.orders.model.OrderLine;
import com.example.orders.model.ProductId;
import com.example.orders.service.CustomerService;
import com.example.orders.service.OrderService;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class OrderController {

    private final OrderService orders;
    private final CustomerService customers;

    public OrderController(OrderService orders, CustomerService customers) {
        this.orders = orders;
        this.customers = customers;
    }

    public List<OrderDto> list() {
        Map<CustomerId, String> names = customers.findAll().stream()
                .collect(Collectors.toMap(Customer::getId, Customer::getName));
        return orders.findAll().stream()
                .map(o -> toDto(o, names.getOrDefault(o.getCustomerId(), "?")))
                .toList();
    }

    public OrderDto get(String id) {
        return withCustomer(orders.get(orderId(id)));
    }

    public OrderDto create(CreateOrderRequest request) {
        return withCustomer(orders.createDraft(new CustomerId(Ids.parse(request.customerId()))));
    }

    public OrderDto addLine(String orderId, AddLineRequest request) {
        return withCustomer(orders.addProduct(orderId(orderId),
                new ProductId(Ids.parse(request.productId())), request.quantity()));
    }

    public OrderDto removeLine(String orderId, String productId) {
        return withCustomer(orders.removeProduct(orderId(orderId), new ProductId(Ids.parse(productId))));
    }

    public OrderDto place(String id) {
        return withCustomer(orders.place(orderId(id)));
    }

    public OrderDto ship(String id) {
        return withCustomer(orders.ship(orderId(id)));
    }

    public OrderDto cancel(String id) {
        return withCustomer(orders.cancel(orderId(id)));
    }

    private OrderDto withCustomer(Order order) {
        return toDto(order, customers.get(order.getCustomerId()).getName());
    }

    private static OrderDto toDto(Order o, String customerName) {
        Function<OrderLine, OrderLineDto> line = l -> new OrderLineDto(l.productId().toString(), l.productName(),
                l.unitPrice().amount(), l.quantity(), l.total().amount());
        return new OrderDto(o.getId().toString(), o.getId().shortForm(), o.getCustomerId().toString(),
                customerName, o.getStatus().name(), o.getCreatedAt(), o.getPlacedAt().orElse(null),
                o.getLines().stream().map(line).toList(), o.total().amount(),
                o.total().currency().getCurrencyCode());
    }

    private static OrderId orderId(String raw) {
        return new OrderId(Ids.parse(raw));
    }
}
