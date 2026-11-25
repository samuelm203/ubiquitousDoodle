package com.example.orders.app.rest;

import com.example.orders.controller.DashboardController;
import com.example.orders.controller.OrderController;
import com.example.orders.controller.dto.Dtos.AddLineRequest;
import com.example.orders.controller.dto.Dtos.CreateOrderRequest;
import com.example.orders.controller.dto.Dtos.DashboardDto;
import com.example.orders.controller.dto.Dtos.OrderDto;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
class OrderRestController {

    private final OrderController orders;
    private final DashboardController dashboard;

    OrderRestController(OrderController orders, DashboardController dashboard) {
        this.orders = orders;
        this.dashboard = dashboard;
    }

    @GetMapping("/dashboard")
    DashboardDto dashboard() {
        return dashboard.overview();
    }

    @GetMapping("/orders")
    List<OrderDto> list() {
        return orders.list();
    }

    @GetMapping("/orders/{id}")
    OrderDto get(@PathVariable String id) {
        return orders.get(id);
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    OrderDto create(@RequestBody CreateOrderRequest request) {
        return orders.create(request);
    }

    @PostMapping("/orders/{id}/lines")
    OrderDto addLine(@PathVariable String id, @RequestBody AddLineRequest request) {
        return orders.addLine(id, request);
    }

    @DeleteMapping("/orders/{id}/lines/{productId}")
    OrderDto removeLine(@PathVariable String id, @PathVariable String productId) {
        return orders.removeLine(id, productId);
    }

    @PostMapping("/orders/{id}/place")
    OrderDto place(@PathVariable String id) {
        return orders.place(id);
    }

    @PostMapping("/orders/{id}/ship")
    OrderDto ship(@PathVariable String id) {
        return orders.ship(id);
    }

    @PostMapping("/orders/{id}/cancel")
    OrderDto cancel(@PathVariable String id) {
        return orders.cancel(id);
    }
}
