package com.example.orders.controller.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Datenobjekte zwischen View und Controller. Sie sind serialisierbar (REST/JSON) und
 * enthalten keine Geschäftslogik. Die Views sehen nie direkt Model-Objekte.
 */
public final class Dtos {

    private Dtos() {
    }

    public record CustomerDto(String id, String name, String email, String city, boolean active) {}

    public record CustomerForm(String name, String email, String city, Boolean active) {}

    public record ProductDto(String id, String sku, String name, BigDecimal price, String currency, boolean active) {}

    public record ProductForm(String sku, String name, BigDecimal price, Boolean active) {}

    public record OrderLineDto(String productId, String productName, BigDecimal unitPrice, int quantity,
                               BigDecimal lineTotal) {}

    public record OrderDto(String id, String number, String customerId, String customerName, String status,
                           Instant createdAt, Instant placedAt, List<OrderLineDto> lines, BigDecimal total,
                           String currency) {}

    public record CreateOrderRequest(String customerId) {}

    public record AddLineRequest(String productId, int quantity) {}

    public record DashboardDto(long customers, long products, Map<String, Long> ordersByStatus,
                               BigDecimal revenue, String currency) {}
}
